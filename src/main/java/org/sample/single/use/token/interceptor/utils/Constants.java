package org.sample.single.use.token.interceptor.utils;

public final class Constants {
    // URL Path for the token revoke endpoint
    public static final String REVOKE_ENDPOINT_CONTEXT = "oauth2/revoke";

    // HTTP POST body parameter values
    public static final String CLIENT_ID_PARAM = "client_id";
    public static final String TOKEN_PARAM = "token";
    public static final String TOKEN_TYPE_HINT_PARAM = "token_type_hint";

    // Token type hint values
    public static final String ACCESS_TOKEN_HINT_VALUE = "access_token";
    public static final String REFRESH_TOKEN_HINT_VALUE = "refresh_token";

    // Commonly used strings
    public static final String COLON_STRING = ":";
    public static final String EQUAL_STRING = "=";
    public static final String AMPERSAND_STRING = "&";

    // Configuration keys
    public static final String USER_AGENT_CONFIG = "UserAgent";
    public static final String IS_BLOCKING_CONFIG = "IsBlocking";

    // Default values for configurations
    public static final String USER_AGENT_CONFIG_DEFAULT = "CustomOAuthTokenInterceptor";
    public static final String IS_BLOCKING_CONFIG_DEFAULT = "false";

    private Constants() {
        // Prevent instantiation
    }
}
