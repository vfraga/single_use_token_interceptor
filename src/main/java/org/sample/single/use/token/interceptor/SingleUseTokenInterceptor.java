package org.sample.single.use.token.interceptor;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.sample.single.use.token.interceptor.utils.Constants;
import org.sample.single.use.token.interceptor.utils.Utils;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.model.IdentityEventListenerConfig;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.oauth.dao.OAuthAppDO;
import org.wso2.carbon.identity.oauth.event.AbstractOAuthEventInterceptor;
import org.wso2.carbon.identity.oauth.event.OAuthEventInterceptor;
import org.wso2.carbon.identity.oauth2.IdentityOAuth2Exception;
import org.wso2.carbon.identity.oauth2.dto.OAuth2IntrospectionResponseDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.identity.oauth2.util.OAuth2Util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SingleUseTokenInterceptor extends AbstractOAuthEventInterceptor {
    private static final Log log = LogFactory.getLog(SingleUseTokenInterceptor.class);

    private void revokeToken(final OAuth2TokenValidationRequestDTO oAuth2TokenValidationRequestDTO,
                             final String consumerKey) throws IdentityOAuth2Exception {
        try {
            // skip extra network hops by using the internal URL instead (usually localhost) of the public URL
            final String internalURL = ServiceURLBuilder.create()
                    .addPath(Constants.REVOKE_ENDPOINT_CONTEXT)
                    .build()
                    .getAbsoluteInternalURL();

            try (final CloseableHttpClient httpclient = Utils.createClientWithCustomVerifier().build()) {
                final OAuthAppDO oAuthAppDO = OAuth2Util.getAppInformationByClientId(consumerKey);

                if (oAuthAppDO == null) {
                    throw new IdentityOAuth2Exception(String.format("No OAuth application found for client ID: %s", consumerKey));
                }

                final HttpPost httpPost = new HttpPost(internalURL);
                final StringBuilder body = new StringBuilder();
                final BasicHttpEntity basicHttpEntity = new BasicHttpEntity();

                final String tokenId = oAuth2TokenValidationRequestDTO.getAccessToken().getIdentifier();
                final String userAgent = getPropertyOrDefault(Constants.USER_AGENT_CONFIG, Constants.USER_AGENT_CONFIG_DEFAULT);

                if (oAuthAppDO.isBypassClientCredentials()) {
                    Utils.addKeyValToBody(Constants.CLIENT_ID_PARAM, consumerKey, body);
                } else {
                    Utils.setAuthorizationHeader(httpPost, consumerKey, oAuthAppDO.getOauthConsumerSecret());
                }

                Utils.addKeyValToBody(Constants.TOKEN_PARAM, tokenId, body);
                // TODO: Should detect whether it's a refresh tokens for better performance?
                Utils.addKeyValToBody(Constants.TOKEN_TYPE_HINT_PARAM, Constants.ACCESS_TOKEN_HINT_VALUE, body);

                httpPost.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
                httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
                httpPost.addHeader(HttpHeaders.USER_AGENT, userAgent);

                basicHttpEntity.setContent(new ByteArrayInputStream(body.toString().getBytes(UTF_8)));
                httpPost.setEntity(basicHttpEntity);

                try (final CloseableHttpResponse response = httpclient.execute(httpPost)) {
                    Utils.handleHttpResponse(response, internalURL);
                } finally {
                    httpPost.releaseConnection();
                }

            } catch (final Exception e) {
                final String msg = e instanceof IOException ?
                        String.format("Error while sending request to '%s'.", internalURL) :
                        String.format("Unexpected error while processing the request to '%s'.", internalURL);

                throwExceptionIfBlocking(e, msg);
            }

        } catch (final URLBuilderException e) {
            final String msg = String.format("Error while building the internal URL with context '%s'.",
                    Constants.REVOKE_ENDPOINT_CONTEXT);

            throwExceptionIfBlocking(e, msg);
        }
    }

    private void throwExceptionIfBlocking(final Exception e, final String msg) throws IdentityOAuth2Exception {
        if (Boolean.parseBoolean(getPropertyOrDefault(Constants.IS_BLOCKING_CONFIG, Constants.IS_BLOCKING_CONFIG_DEFAULT))) {
            // If blocking is enabled, throw an exception to prevent further processing.
            throw new IdentityOAuth2Exception(msg, e);
        }
        // Otherwise, log the error and continue.
        log.warn(msg, e);
    }

    private String getPropertyOrDefault(final String key, final String defaultValue) {
        final IdentityEventListenerConfig config = getIdentityEventListenerConfig();
        return config == null ? defaultValue : config.getProperties().getProperty(key);
    }

    private IdentityEventListenerConfig getIdentityEventListenerConfig() {
        return IdentityUtil.readEventListenerProperty(OAuthEventInterceptor.class.getName(), SingleUseTokenInterceptor.class.getName());
    }

    @Override
    public boolean isEnabled() {
        final IdentityEventListenerConfig config = getIdentityEventListenerConfig();
        // defaults to false if not configured
        return config != null && Boolean.parseBoolean(config.getEnable());
    }

    @Override
    public void onPostTokenValidation(final OAuth2TokenValidationRequestDTO validationReqDTO,
                                      final OAuth2TokenValidationResponseDTO validationResponseDTO,
                                      final Map<String, Object> params) {
        // only called by the userinfo endpoint. revoking prematurely will cause errors downstream.
    }

    @Override
    public void onPostTokenValidation(final OAuth2TokenValidationRequestDTO oAuth2TokenValidationRequestDTO,
                                      final OAuth2IntrospectionResponseDTO oAuth2IntrospectionResponseDTO,
                                      final Map<String, Object> params) throws IdentityOAuth2Exception {
        if (oAuth2IntrospectionResponseDTO.isActive() &&
                StringUtils.isNotBlank(oAuth2IntrospectionResponseDTO.getUsername())) {
            // revoke token if no errors during validation. token data object should've been retrieved by now.
            revokeToken(oAuth2TokenValidationRequestDTO, oAuth2IntrospectionResponseDTO.getClientId());
        }
    }
}
