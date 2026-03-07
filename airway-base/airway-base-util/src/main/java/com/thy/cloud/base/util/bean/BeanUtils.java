package com.thy.cloud.base.util.bean;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;

/**
 * BeanUtils extending Spring's BeanUtils with null-property detection.
 *
 * @author Engin Mahmut
 */
public class BeanUtils extends org.springframework.beans.BeanUtils {

    /**
     * Returns property names that have null values.
     */
    public static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        return emptyNames.toArray(new String[0]);
    }

    /**
     * Copy properties ignoring null values.
     */
    public static void copyPropertiesIgnoreNull(Object src, Object target) {
        copyProperties(src, target, getNullPropertyNames(src));
    }

    /**
     * Get a named property value from a bean (compatible with Apache Commons
     * BeanUtils API).
     */
    public static Object getProperty(Object bean, String name) {
        if (bean == null || name == null) {
            return null;
        }
        final BeanWrapper wrapper = new BeanWrapperImpl(bean);
        return wrapper.getPropertyValue(name);
    }
}
