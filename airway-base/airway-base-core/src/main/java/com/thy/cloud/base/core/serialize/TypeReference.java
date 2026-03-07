package com.thy.cloud.base.core.serialize;

import lombok.Getter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Generic type reference for preserving generic type information at runtime.
 * <p>
 * Usage: {@code new TypeReference<List<String>>() {}}
 *
 * @author Engin Mahmut
 */
@Getter
public abstract class TypeReference<T> implements Comparable<TypeReference<T>> {

    protected final Type type;

    protected TypeReference() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof Class<?>) {
            throw new IllegalArgumentException(
                    "TypeReference constructed without actual type information");
        }
        type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    @Override
    public int compareTo(TypeReference<T> o) {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
