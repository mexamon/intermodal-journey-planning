package com.thy.cloud.base.core.context;

public class BaseContextConstants {

    private BaseContextConstants(){
        throw new IllegalStateException("Utility Class");
    }

    /**
     * User id encapsulated in JWT
     */
    public static final String JWT_KEY_USER_ID = "userid";
    /**
     * Username encapsulated in JWT
     */
    public static final String JWT_KEY_NAME = "name";
    /**
     * Token type encapsulated in JWT
     */
    public static final String JWT_KEY_TOKEN_TYPE = "token_type";
    /**
     * User account encapsulated in JWT
     */
    public static final String JWT_KEY_ACCOUNT = "account";

    /**
     * JClient id encapsulated in JWT
     */
    public static final String JWT_KEY_CLIENT_ID = "client_id";

    /**
     *JWT token signature
     */
    public static final String JWT_SIGN_KEY = "airway";

    /**
     * Tenant code encapsulated in JWT
     */
    public static final String JWT_KEY_TENANT = "tenant";
    /**
     * Refresh Token
     */
    public static final String REFRESH_TOKEN_KEY = "refresh_token";

    /**
     * User information Authentication request header
     */
    public static final String BEARER_HEADER_KEY = "token";
    /**
     * User information Authentication request header prefix
     */
    public static final String BEARER_HEADER_PREFIX = "Bearer ";

    /**
     * Client information authentication request header
     */
    public static final String BASIC_HEADER_KEY = "Authorization";

    /**
     * Client information authentication request header prefix
     */
    public static final String BASIC_HEADER_PREFIX = "Basic ";

    /**
     * Client information authentication request header prefix
     */
    public static final String BASIC_HEADER_PREFIX_EXT = "Basic%20";

    /**
     * Whether to boot project
     */
    public static final String IS_BOOT = "boot";

    /**
     * Log link tracking id header
     */
    public static final String TRACE_ID_HEADER = "x-trace-header";
    /**
     * Log link tracking id log flag
     */
    public static final String LOG_TRACE_ID = "trace";

    /**
     * token
     */
    @Deprecated
    public static final String TOKEN_NAME = BEARER_HEADER_KEY;

}
