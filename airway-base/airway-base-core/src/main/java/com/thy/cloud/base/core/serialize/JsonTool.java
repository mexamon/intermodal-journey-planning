package com.thy.cloud.base.core.serialize;

import java.lang.reflect.Type;

/**
 * JSON tool abstraction — adapter interface for serialization operations.
 *
 * @author Engin Mahmut
 */
public interface JsonTool {

    /**
     * Serialize object to JSON string.
     *
     * @param obj the object to serialize
     * @return JSON string representation
     */
    String toJson(Object obj);

    /**
     * Deserialize JSON string to object.
     *
     * @param json  JSON string
     * @param clazz target class
     * @return deserialized object
     */
    <T> T toObj(String json, Class<T> clazz);

    /**
     * Deserialize JSON string to object using generic type.
     *
     * @param json JSON string
     * @param type target type
     * @return deserialized object
     */
    <T> T toObj(String json, Type type);

    /**
     * Deserialize JSON string to object using TypeReference.
     *
     * @param json          JSON string
     * @param typeReference type reference for generic types
     * @return deserialized object
     */
    <T> T toObj(String json, TypeReference<T> typeReference);
}
