package com.thy.cloud.base.core.enums;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.util.Assert;

import java.util.stream.Stream;

/**
 * Interface for generic value enum
 *
 * @param <T> Enum Value Type
 *
 * @author Engin Mahmut
 */
public interface IEnumI18n<T> {

    /**
     * Value information
     * 
     * @return T
     */
    T getValue();

    /**
     * Convert Set of Enumerations
     * key -> name
     * value -> desc
     *
     * @return Localization name with enum prefix
     */
    default String getDesc() {
        return this.getClass().getSimpleName() + '.' + this.name().toLowerCase();
    }

    /**
     * Enum Name
     * 
     * @return String
     */
    String name();

    /**
     * Get Enum Name from name()
     * 
     * @return String
     */
    default String getCode() {
        return toString();
    }

    /**
     * Converts value to corresponding enum.
     *
     * @param type  enum type
     * @param value value
     * @param <V>   value generic
     * @param <E>   enum generic
     * @return corresponding enum
     **/
    static <V, E extends IEnumI18n<V>> E valueToEnum(Class<E> type, V value) {

        Assert.notNull(type, "enum type must not be null");
        Assert.notNull(value, "enum value must not be null");
        Assert.isTrue(type.isEnum(), "enum type must be an enum type");

        return Stream
                .of(type.getEnumConstants())
                .filter(item -> item.getValue().equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown value: " + value));
    }

    /**
     * Convert Set of Enumerations"
     * key -> name
     * value -> desc
     *
     * resource file names should be same with enum name.
     * In order to handle name correctly use locale for get name of enum
     * this.name().toLowerCase(Locale.ENGLISH)
     *
     * @return Localization name with enum prefix
     */
    @JsonIgnore
    default String getMessageKey() {
        return this.getClass().getSimpleName() + '.' + this.name();
    }

}
