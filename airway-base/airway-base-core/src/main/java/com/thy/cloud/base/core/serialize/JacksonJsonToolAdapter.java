package com.thy.cloud.base.core.serialize;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.lang.reflect.Type;

/**
 * Jackson-based JSON tool implementation.
 * <p>
 * Uses the globally configured {@link ObjectMapper} from
 * {@link SerializationConfig}.
 *
 * @author Engin Mahmut
 */
public class JacksonJsonToolAdapter implements JsonTool {

    @Getter
    @Setter
    private ObjectMapper mapper;

    public JacksonJsonToolAdapter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @SneakyThrows(JacksonException.class)
    @Override
    public String toJson(Object obj) {
        return mapper.writeValueAsString(obj);
    }

    @SneakyThrows(JacksonException.class)
    @Override
    public <T> T toObj(String json, Class<T> clazz) {
        return mapper.readValue(json, clazz);
    }

    @SneakyThrows(JacksonException.class)
    @Override
    public <T> T toObj(String json, Type type) {
        return mapper.readValue(json, mapper.constructType(type));
    }

    @SneakyThrows(JacksonException.class)
    @Override
    public <T> T toObj(String json, TypeReference<T> typeReference) {
        return mapper.readValue(json, new tools.jackson.core.type.TypeReference<T>() {
            @Override
            public Type getType() {
                return typeReference.getType();
            }
        });
    }
}
