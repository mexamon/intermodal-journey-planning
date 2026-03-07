package com.thy.cloud.base.util.type;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Reflection Type Utility
 *
 * @author Engin Mahmut
 */
public class TypeUtils {

    private TypeUtils() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public static ParameterizedType getParameterizedType(@NonNull Class<?> superType, Type... genericTypes) {
        Assert.notNull(superType, "Interface or super type must not be null");

        ParameterizedType currentType = null;
        for (Type genericType : genericTypes) {
            if (genericType instanceof ParameterizedType parameterizedType) {
                if (parameterizedType.getRawType().getTypeName().equals(superType.getTypeName())) {
                    currentType = parameterizedType;
                    break;
                }
            }
        }
        return currentType;
    }

    @Nullable
    public static ParameterizedType getParameterizedType(@NonNull Class<?> interfaceType,
            Class<?> implementationClass) {
        Assert.notNull(interfaceType, "Interface type must not be null");
        Assert.isTrue(interfaceType.isInterface(), "The given type must be an interface");

        if (implementationClass == null) {
            return null;
        }

        ParameterizedType currentType = getParameterizedType(interfaceType, implementationClass.getGenericInterfaces());
        if (currentType != null) {
            return currentType;
        }

        return getParameterizedType(interfaceType, implementationClass.getSuperclass());
    }

    @Nullable
    public static ParameterizedType getParameterizedTypeBySuperClass(@NonNull Class<?> superClassType,
            Class<?> extensionClass) {
        if (extensionClass == null) {
            return null;
        }
        return getParameterizedType(superClassType, extensionClass.getGenericSuperclass());
    }

    public static Object getFieldValue(@NonNull String fieldName, @NonNull Object object) {
        Assert.notNull(fieldName, "FieldName must not be null");
        Assert.notNull(object, "Object type must not be null");
        try {
            String firstLetter = fieldName.substring(0, 1).toUpperCase();
            String getter = "get" + firstLetter + fieldName.substring(1);
            Method method = object.getClass().getMethod(getter);
            return method.invoke(object);
        } catch (Exception e) {
            return null;
        }
    }
}
