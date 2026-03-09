package com.thy.cloud.data.cache.serializer;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

/**
 * Redis serializer that delegates to Spring Data Redis's
 * {@link GenericJacksonJsonRedisSerializer} with default typing enabled
 * and a custom enum module that forces enum values to serialize as
 * their {@code name()} string, bypassing {@code @JsonFormat(shape=OBJECT)}.
 *
 * @author Engin Mahmut
 */
public class RedisObjectSerializer implements RedisSerializer<Object> {

    private final GenericJacksonJsonRedisSerializer delegate;

    @SuppressWarnings("rawtypes")
    public RedisObjectSerializer(ObjectMapper objectMapper) {
        var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubTypeIsArray()
                .build();

        // Serialize enums as name() strings in Redis, overriding
        // @JsonFormat(shape=OBJECT) which is kept for API responses.
        SimpleModule enumModule = new SimpleModule("RedisEnumOverride");
        enumModule.addSerializer(Enum.class, EnumNameSerializer.INSTANCE);

        this.delegate = GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(ptv)
                .customize(builder -> builder.addModule(enumModule))
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

    @SuppressWarnings("rawtypes")
    static class EnumNameSerializer extends StdSerializer<Enum> {
        static final EnumNameSerializer INSTANCE = new EnumNameSerializer();

        EnumNameSerializer() {
            super(Enum.class);
        }

        @Override
        public void serialize(Enum value, JsonGenerator gen, SerializationContext ctx) {
            gen.writeString(value.name());
        }
    }
}
