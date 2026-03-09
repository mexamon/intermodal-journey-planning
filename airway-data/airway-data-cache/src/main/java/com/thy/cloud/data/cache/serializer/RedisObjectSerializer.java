package com.thy.cloud.data.cache.serializer;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * Redis serializer that delegates to Spring Data Redis's
 * {@link GenericJacksonJsonRedisSerializer} with default typing enabled.
 * <p>
 * Enums are serialized as strings (their name()) by default since
 * {@code @JsonFormat(shape=OBJECT)} was removed — the API's
 * {@code AirwayJacksonModule.IEnumSerializer} handles rich enum
 * formatting for API responses only.
 *
 * @author Engin Mahmut
 */
public class RedisObjectSerializer implements RedisSerializer<Object> {

    private final GenericJacksonJsonRedisSerializer delegate;

    public RedisObjectSerializer(ObjectMapper objectMapper) {
        var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubTypeIsArray()
                .build();

        this.delegate = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(ptv)
                .build();
    }

    @Override
    public byte[] serialize(Object value) throws SerializationException {
        return delegate.serialize(value);
    }

    @Override
    public Object deserialize(byte[] bytes) throws SerializationException {
        return delegate.deserialize(bytes);
    }
}
