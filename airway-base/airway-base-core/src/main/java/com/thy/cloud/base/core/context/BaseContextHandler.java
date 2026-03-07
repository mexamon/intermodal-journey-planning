package com.thy.cloud.base.core.context;

import com.thy.cloud.base.util.math.NumberHelper;
import com.thy.cloud.base.util.str.StrHelper;

import java.util.HashMap;
import java.util.Map;


/**
 * Get the user id appid user nickname in the current domain
 * Note: The appid is parsed by token, and the user id and user nickname must be passed in through the request header at the front end. Otherwise it is not available here
 *
 */
public class BaseContextHandler {
    public static final ThreadLocal<Map<String, String>> THREAD_LOCAL = new ThreadLocal<>();

    public static void set(String key, Long value) {
        Map<String, String> map = getLocalMap();
        map.put(key, value == null ? "0" : String.valueOf(value));
    }

    public static void set(String key, String value) {
        Map<String, String> map = getLocalMap();
        map.put(key, value == null ? "" : value);
    }

    private static Map<String, String> getLocalMap() {
        Map<String, String> map = THREAD_LOCAL.get();
        if (map == null) {
            map = new HashMap<>(10);
            THREAD_LOCAL.set(map);
        }
        return map;
    }

    public static String get(String key) {
        Map<String, String> map = getLocalMap();
        return map.getOrDefault(key, "");
    }

    /**
     * Account id
     *
     * @return
     */
    public static Long getUserId() {
        Object value = get(BaseContextConstants.JWT_KEY_USER_ID);
        return NumberHelper.longValueOf0(value);
    }

    /**
     * Account id
     *
     * @param userId
     */
    public static void setUserId(Long userId) {
        set(BaseContextConstants.JWT_KEY_USER_ID, userId);
    }

    public static void setUserId(String userId) {
        setUserId(NumberHelper.longValueOf0(userId));
    }

    /**
     * Name in the account table
     *
     * @return
     */
    public static String getAccount() {
        Object value = get(BaseContextConstants.JWT_KEY_ACCOUNT);
        return returnObjectValue(value);
    }

    /**
     * Name in the account table
     *
     * @param name
     */
    public static void setAccount(String name) {
        set(BaseContextConstants.JWT_KEY_ACCOUNT, name);
    }


    /**
     * Login account
     *
     * @return
     */
    public static String getName() {
        Object value = get(BaseContextConstants.JWT_KEY_NAME);
        return returnObjectValue(value);
    }

    /**
     * Login account
     *
     * @param account
     */
    public static void setName(String account) {
        set(BaseContextConstants.JWT_KEY_NAME, account);
    }

    /**
     * Get user token
     *
     * @return
     */
    public static String getToken() {
        Object value = get(BaseContextConstants.TOKEN_NAME);
        return StrHelper.getObjectValue(value);
    }

    public static void setToken(String token) {
        set(BaseContextConstants.TOKEN_NAME, token);
    }


    public static String getTeßnant() {
        Object value = get(BaseContextConstants.JWT_KEY_TENANT);
        return StrHelper.getObjectValue(value);
    }

    public static void setTenant(String val) {
        set(BaseContextConstants.JWT_KEY_TENANT, val);
    }

    private static String returnObjectValue(Object value) {
        return value == null ? "" : value.toString();
    }

    public static void remove() {
        if (THREAD_LOCAL != null) {
            THREAD_LOCAL.remove();
        }
    }
}
