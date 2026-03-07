package com.thy.cloud.base.core.context;

import cn.hutool.core.convert.Convert;
import com.thy.cloud.base.util.str.StrPool;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Get the user id, user nickname, tenant code, account number and other information in the current thread variables
 *
 * @author Engin Mahmut
 *
 */
public final class ContextUtil {
    private ContextUtil() {
    }

    private static final ThreadLocal<Map<String, String>> THREAD_LOCAL = new ThreadLocal<>();

    public static void set(String key, Object value) {
        Map<String, String> map = getLocalMap();
        map.put(key, value == null ? StrPool.EMPTY : value.toString());
    }

    public static <T> T get(String key, Class<T> type) {
        Map<String, String> map = getLocalMap();
        return Convert.convert(type, map.get(key));
    }

    public static <T> T get(String key, Class<T> type, Object def) {
        Map<String, String> map = getLocalMap();
        return Convert.convert(type, map.getOrDefault(key, String.valueOf(def == null ? StrPool.EMPTY : def)));
    }

    public static String get(String key) {
        Map<String, String> map = getLocalMap();
        return map.getOrDefault(key, StrPool.EMPTY);
    }

    public static Map<String, String> getLocalMap() {
        Map<String, String> map = THREAD_LOCAL.get();
        if (map == null) {
            map = new ConcurrentHashMap<>(10);
            THREAD_LOCAL.set(map);
        }
        return map;
    }

    public static void setLocalMap(Map<String, String> localMap) {
        THREAD_LOCAL.set(localMap);
    }


    /**
     * 是否boot项目
     *
     * @return 是否boot项目
     */
    public static Boolean getBoot() {
        return get(BaseContextConstants.IS_BOOT, Boolean.class, false);
    }

    public static void setBoot(Boolean val) {
        set(BaseContextConstants.IS_BOOT, val);
    }

    /**
     * 用户ID
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        return get(BaseContextConstants.JWT_KEY_USER_ID, Long.class, 0L);
    }

    public static String getUserIdStr() {
        return String.valueOf(getUserId());
    }

    /**
     * 用户ID
     *
     * @param userId 用户ID
     */
    public static void setUserId(Long userId) {
        set(BaseContextConstants.JWT_KEY_USER_ID, userId);
    }

    public static void setUserId(String userId) {
        set(BaseContextConstants.JWT_KEY_USER_ID, userId);
    }

    /**
     * 登录账号
     *
     * @return 登录账号
     */
    public static String getAccount() {
        return get(BaseContextConstants.JWT_KEY_ACCOUNT, String.class);
    }

    /**
     * 登录账号
     *
     * @param account 登录账号
     */
    public static void setAccount(String account) {
        set(BaseContextConstants.JWT_KEY_ACCOUNT, account);
    }


    /**
     * 用户姓名
     *
     * @return 用户姓名
     */
    public static String getName() {
        return get(BaseContextConstants.JWT_KEY_NAME, String.class);
    }

    /**
     * 用户姓名
     *
     * @param name 用户姓名
     */
    public static void setName(String name) {
        set(BaseContextConstants.JWT_KEY_NAME, name);
    }

    /**
     * 获取token
     *
     * @return token
     */
    public static String getToken() {
        return get(BaseContextConstants.BEARER_HEADER_KEY, String.class);
    }

    public static void setToken(String token) {
        set(BaseContextConstants.BEARER_HEADER_KEY, token);
    }

    /**
     * 获取租户编码
     *
     * @return 租户编码
     */
    public static String getTenant() {
        return get(BaseContextConstants.JWT_KEY_TENANT, String.class, StrPool.EMPTY);
    }

    public static void setTenant(String val) {
        set(BaseContextConstants.JWT_KEY_TENANT, val);
    }

    public static String getClientId() {
        return get(BaseContextConstants.JWT_KEY_CLIENT_ID, String.class);
    }

    public static void setClientId(String val) {
        set(BaseContextConstants.JWT_KEY_CLIENT_ID, val);
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }

}
