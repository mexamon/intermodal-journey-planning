package com.thy.cloud.data.cache.serializer;

import tools.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * {@link RedisSerializer} implementation using Jackson 3 {@link GenericJacksonJsonRedisSerializer}
 * with custom {@link ObjectMapper} configuration.
 * <p>
 * Spring Data Redis 4 provides {@code GenericJacksonJsonRedisSerializer} as the
 * Jackson 3 replacement for the deprecated {@code GenericJackson2JsonRedisSerializer}.
 *
 * @author Engin Mahmut
 */
public class RedisObjectSerializer implements RedisSerializer<Object> {

    private final GenericJacksonJsonRedisSerializer delegate;

    public RedisObjectSerializer(ObjectMapper objectMapper) {
        this.delegate = new GenericJacksonJsonRedisSerializer(objectMapper);
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
