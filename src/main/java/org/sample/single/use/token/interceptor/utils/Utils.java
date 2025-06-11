package org.sample.single.use.token.interceptor.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;


public final class Utils {
    public static final String DEFAULT_AND_LOCALHOST = "DefaultAndLocalhost";
    public static final String ALLOW_ALL = "AllowAll";
    public static final String HOST_NAME_VERIFIER = "httpclient.hostnameVerifier";

    private static final Log log = LogFactory.getLog(Utils.class);

    private Utils() {
        // Prevent instantiation
    }

    public static HttpClientBuilder createClientWithCustomVerifier() {
        final HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().useSystemProperties();

        if (DEFAULT_AND_LOCALHOST.equals(System.getProperty(HOST_NAME_VERIFIER))) {
            log.debug("Allowing default CN and loopback hostnames.");
            httpClientBuilder.setHostnameVerifier(new CustomHostnameVerifier());
        } else if (ALLOW_ALL.equals(System.getProperty(HOST_NAME_VERIFIER))) {
            log.debug("Using no hostname verification.");
            httpClientBuilder.setHostnameVerifier(new AllowAllHostnameVerifier());
        } else {
            log.debug("Using strict verification.");
            httpClientBuilder.setHostnameVerifier(new StrictHostnameVerifier());
        }

        return httpClientBuilder;
    }

    public static void handleHttpResponse(final CloseableHttpResponse response, final String url) {
        if (response == null) {
            log.warn("Response object is null for URL: " + url);
            return;
        }

        final int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == HttpStatus.SC_OK) {
            log.info(String.format("Request to '%s' was successful with status: %s.", url, statusCode));
        } else {
            log.error(String.format("Request to '%s' failed with status: %s", url, statusCode));
        }
    }

    public static void setAuthorizationHeader(final HttpRequestBase httpMethod,
                                              final String clientKey,
                                              final String clientSecret) {
        if (StringUtils.isBlank(clientKey) || StringUtils.isBlank(clientSecret) || httpMethod == null) {
            log.debug("Client key, client secret or HttpRequestBase is null or empty.");
            return;
        }

        final String toEncode = clientKey + Constants.COLON_STRING + clientSecret;
        final byte[] encoding = Base64.getEncoder().encode(toEncode.getBytes());
        final String authHeader = new String(encoding, UTF_8);

        httpMethod.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + authHeader);
    }


    public static void addKeyValToBody(final String key, final String val, final StringBuilder body) {
        if (StringUtils.isBlank(key) || StringUtils.isBlank(val) || body == null) {
            log.debug("Couldn't add to body as key, value or body is null or empty.");
            return;
        }

        if (body.length() == 0 || body.charAt(body.length() - 1) != Constants.AMPERSAND_STRING.charAt(0)) {
            body.append(Constants.AMPERSAND_STRING);
        }

        body.append(key).append(Constants.EQUAL_STRING).append(val);
    }
}
