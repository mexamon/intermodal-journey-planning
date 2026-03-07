package com.thy.cloud.base.core.enums;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class IEnumUtils<T extends IEnum<T>>
{

    private IEnumUtils() {
    }

    /**
     * BaseEnum.getCode() is used as the Key, BaseEnum.getDesc() is used as the value, stored in the Map and returned.
     *
     * @param <T>
     * @param enumClass
     * @return
     */
    public static <T extends IEnum<T>> LinkedHashMap<Object,String> toMap(Class<? extends IEnum> enumClass) {
        return toMap(enumClass.getEnumConstants());
    }

    /**
     * BaseEnum.getCode() is used as the Key, BaseEnum.getDesc() is used as the value, stored in the Map and returned
     *
     * @param <T>
     * @param values
     * @return
     */
    public static <T extends IEnum<T>> LinkedHashMap<Object,String> toMap(T[] values) {
        return Arrays.stream(values).collect(Collectors.toMap(IEnum::getValue, IEnum::getDesc, (a, b) -> b, LinkedHashMap::new));
    }

    public static <T extends IEnum<T>> Object getCode(T kv) {
        if (kv == null)
            return null;
        return kv.getValue();
    }

    public static <T extends IEnum> String getDesc(T kv) {
        if (kv == null)
            return null;
        return kv.getDesc();
    }

    public static <T extends IEnum> String getName(T kv) {
        if (kv == null)
            return null;
        return kv.name();
    }

    /**
     * Find Enum based on code
     *
     * @param code
     * @param enumClass
     * @return
     */
    public static <T extends IEnum> T getByCode(Object code, Class<? extends IEnum> enumClass) {
        return (T) getByCode(code, enumClass.getEnumConstants());
    }

    /**
     * Find Enum based on code
     *
     * @param code
     * @param values
     * @return
     */
    public static <T extends IEnum> T getByCode(Object code, T[] values) {
        if (code == null)
            return null;
        for (T item : values) {
            if (item.getValue().equals(code)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Find Enum based on code
     *
     * @param code
     * @param enumClass
     * @return
     */
    public static <T extends IEnum> T getRequiredByCode(Object code, Class<? extends IEnum> enumClass) {
        return (T) getRequiredByCode(code, enumClass.getEnumConstants());
    }

    /**
     * According to the code to get Enum, if not found, throw an exception
     *
     * @param <T>
     * @param code
     * @param values
     * @return
     * @throws IllegalArgumentException According to the code to get Enum, if not found, throw an exception
     */
    public static <T extends IEnum> T getRequiredByCode(Object code, T[] values) throws IllegalArgumentException {
        IEnum v = getByCode(code, values);
        if (v == null) {
            if (values.length > 0) {
                String className = values[0].getClass().getName();
                throw new IllegalArgumentException("not found " + className + " value by code:" + code);
            } else {
                throw new IllegalArgumentException("not found Enum by code:" + code);
            }
        }
        return (T) v;
    }

    public static boolean isDefined(Enum<?>[] ems, String emStr) {
        for (Enum<?> em : ems) {
            if (em.toString().equals(emStr))
                return true;
        }
        return false;
    }
}