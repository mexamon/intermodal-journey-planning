package com.thy.cloud.base.core.enums;

import com.thy.cloud.base.util.map.MapHelper;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Interface for generic value enum
 *
 * @param <T> Enum Value Type
 *
 * @author Engin Mahmut
 */
public interface IEnum<T> {

    /**
     * Value information
     * @return T
     */
    T getValue();

    /**
     * Description
     * @return String
     */
    String getDesc();

    /**
     * Enum Name
     * @return String
     */
    String name();

    /**
     * Get Enum Name from name()
     * @return String
     */
    default String getCode() {
        return toString();
    }

    /**
     * Converts value to corresponding enum.
     *
     * @param type enum type
     * @param value    value
     * @param <V>      value generic
     * @param <E>      enum generic
     * @return corresponding enum
     **/
    static <V, E extends IEnum<V>> E valueToEnum(Class<E> type, V value) {

        Assert.notNull(type, "enum type must not be null");
        Assert.notNull(value, "enum value must not be null");
        Assert.isTrue(type.isEnum(), "enum type must be an enum type");

        return Stream.of(type.getEnumConstants())
                .filter(item -> item.getValue().equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown database value: " + value));

    }

    /**
     * Convert Set of Enumerations"
     * key -> name
     * value -> desc
     *
     * @param list IEnum list
     * @return map
     */
    static Map<String, String> getMap(IEnum[] list) {
        return MapHelper.uniqueIndex(Arrays.asList(list), IEnum::getCode, IEnum::getDesc);
    }

}

