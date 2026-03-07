package com.thy.cloud.base.core.serialize;

import lombok.Getter;

import java.lang.reflect.Type;

/**
 * Static JSON utility class backed by Jackson.
 * <p>
 * Initialized by {@link SerializationConfig} during auto-configuration.
 * Provides a convenient static API for JSON operations project-wide.
 *
 * @author Engin Mahmut
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    @Getter
    private static JsonTool jsonTool;

    /**
     * Called by {@link SerializationConfig} during startup.
     */
    static void init(JsonTool tool) {
        JsonUtils.jsonTool = tool;
    }

    public static String toJson(Object obj) {
        return jsonTool.toJson(obj);
    }

    public static <T> T toObj(String json, Class<T> clazz) {
        return jsonTool.toObj(json, clazz);
    }

    public static <T> T toObj(String json, Type type) {
        return jsonTool.toObj(json, type);
    }

    public static <T> T toObj(String json, TypeReference<T> typeReference) {
        return jsonTool.toObj(json, typeReference);
    }
}
