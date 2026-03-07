package com.thy.cloud.base.core.enums;

import com.thy.cloud.base.util.type.TypeUtils;

import jakarta.persistence.AttributeConverter;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Abstract converter for Generic Enum
 * @see IEnum
 * @author Engin Mahmut
 *
 */
/*@Converter(autoApply = true)*/
public abstract class AbstractEnumConverter<E extends IEnum<V>, V> implements AttributeConverter<E, V> {

    private final Class<E> clazz;

    @SuppressWarnings("unchecked")
    protected AbstractEnumConverter() {
        Type enumType = Objects.requireNonNull(
                TypeUtils.getParameterizedTypeBySuperClass(AbstractEnumConverter.class, this.getClass())
        ).getActualTypeArguments()[0];
        this.clazz = (Class<E>) enumType;
    }

    @Override
    public V convertToDatabaseColumn(E attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public E convertToEntityAttribute(V dbData) {
        return dbData == null ? null : IEnum.valueToEnum(clazz, dbData);
    }

}
