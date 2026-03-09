package com.thy.cloud.data.cache.serializer;

import tools.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * {@link RedisSerializer} implementation using Jackson 3 {@link GenericJacksonJsonRedisSerializer}
 * with default typing enabled for proper polymorphic deserialization.
 * <p>
 * Uses Spring Data Redis 4's builder pattern to enable {@code @class} type metadata
 * in serialized JSON, ensuring cached entities reconstruct to their correct Java types.
 *
 * @author Engin Mahmut
 */
public class RedisObjectSerializer implements RedisSerializer<Object> {

    private final GenericJacksonJsonRedisSerializer delegate;

    public RedisObjectSerializer(ObjectMapper objectMapper) {
        this.delegate = GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
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
