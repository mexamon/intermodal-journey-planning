package com.thy.cloud.data.jpa.util;

import com.thy.cloud.base.util.date.DateUtil;
import com.thy.cloud.base.util.str.StringUtils;
import com.thy.cloud.data.jpa.constant.PersistenceConstants;
import com.thy.cloud.data.jpa.entity.AbstractAuditionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
public class GenericSpecification<T extends AbstractAuditionEntity> {

    public static Specification getSpecification(AbstractAuditionEntity t) {
        return (Specification<AbstractAuditionEntity>) (root, query, cb) -> {
            List<Predicate> list = GenericSpecification.getFieldValue(t, root, cb);
            Predicate[] pre = new Predicate[list.size()];
            pre = list.toArray(pre);
            return query.where(pre).getRestriction();
        };
    }

    /**
     *
     * if (Constant.BASIC_TYPE_INTEGER.equals(fieldType)) {
     * lp.add(cb.equal(root.get(fields[i].getName()).as(Integer.class),
     * Integer.valueOf(value)));
     * } else if("BigDecimal".equals(fieldType)) {
     * lp.add(cb.equal(root.get(fields[i].getName()).as(BigDecimal.class), new
     * BigDecimal(value)));
     * } else if("Long".equals(fieldType)) {
     * lp.add(cb.equal(root.get(fields[i].getName()).as(Long.class),
     * Long.valueOf(value)));
     * } else if("Date".equals(fieldType)) {
     * lp.add(cb.equal(root.get(fields[i].getName()).as(Date.class),
     * DateUtils.parseDate(value)));
     * } else if("int".equals(fieldType)) {
     * lp.add(cb.equal(root.get(fields[i].getName()).as(Integer.class),
     * Integer.valueOf(value)));
     * } else if("String".equals(fieldType)) {
     * lp.add(cb.equal(root.get(fields[i].getName()).as(String.class), value));
     * }
     * 
     * @Description
     * @param entity
     * @param root
     * @param cb
     * @return void
     * @throws
     */
    public static List<Predicate> getFieldValue(AbstractAuditionEntity entity, Root<?> root, CriteriaBuilder cb) {
        List<Predicate> lp = new ArrayList<>();
        Class<?> cls = entity.getClass();
        Field[] fields = cls.getDeclaredFields();
        Method[] methods = cls.getDeclaredMethods();
        Predicate predicate = null;
        for (Field field : fields) {
            try {
                // Verify whether there are GETTER, SETTER methods
                if (!checkGetSetMethodAndAnnotation(methods, field)) {
                    continue;
                }
                String fieldGetName = parGetName(field.getName());
                String fieldSetName = parSetName(field.getName());

                Method fieldSetMet = cls.getMethod(fieldGetName);

                Object o = fieldSetMet.invoke(entity);
                // Type conversion
                if (null == o) {
                    continue;
                }
                String fieldType = field.getType().getSimpleName();
                String value = o.toString();
                switch (fieldType) {
                    case PersistenceConstants.BASIC_TYPE_INTEGER:
                        predicate = doInteger(cb, root, field, value);
                        break;
                    case PersistenceConstants.BASIC_TYPE_BIGDECIMAL:
                        predicate = cb.equal(root.get(field.getName()).as(BigDecimal.class), new BigDecimal(value));
                        break;
                    case PersistenceConstants.BASIC_TYPE_Long:
                        predicate = cb.equal(root.get(field.getName()).as(Long.class), Long.valueOf(value));
                        break;
                    case PersistenceConstants.BASIC_TYPE_DATE:
                        predicate = cb.equal(root.get(field.getName()).as(Date.class), DateUtil.parseDate(value));
                        break;
                    case PersistenceConstants.BASIC_TYPE_INT:
                        predicate = doInteger(cb, root, field, value);
                        break;
                    default:
                        predicate = cb.equal(root.get(field.getName()).as(String.class), value);
                        break;
                }
                lp.add(predicate);
            } catch (NoSuchMethodException e) {
                log.error(e.getMessage(), e.getCause());
                continue;
            } catch (IllegalAccessException e) {
                log.error(e.getMessage(), e.getCause());
                continue;
            } catch (InvocationTargetException e) {
                log.error(e.getMessage(), e.getCause());
                continue;
            }
        }
        return lp;
    }

    /**
     * Check the Set Get method and ignore the annotation of Transient
     * 
     * @Description
     * @param methods
     * @param field
     * @return boolean
     * @throws
     */
    public static boolean checkGetSetMethodAndAnnotation(Method[] methods, Field field) {
        boolean result = true;
        String fieldGetName = parGetName(field.getName());
        String fieldSetName = parSetName(field.getName());
        // Verify whether there are GETTER, SETTER methods
        if (!checkGetMet(methods, fieldGetName) || !checkSetMet(methods, fieldSetName)) {
            result = false;
        } else {
            // Check Transient annotation
            Annotation annotation = field.getAnnotation(jakarta.persistence.Transient.class);
            if (annotation != null) {
                result = false;
            }
        }
        return result;
    }

    /**
     * @Description
     * @param cb
     * @param root
     * @param field
     * @param value
     * @return jakarta.persistence.criteria.Predicate
     * @throws
     */
    public static Predicate doInteger(CriteriaBuilder cb, Root<?> root, Field field, String value) {
        return cb.equal(root.get(field.getName()).as(Integer.class), Integer.valueOf(value));
    }

    /**
     * The get method of splicing an attribute
     *
     * @param fieldName
     * @return String
     */
    public static String parGetName(String fieldName) {
        if (null == fieldName || "".equals(fieldName)) {
            return null;
        }
        return "get" + fieldName.substring(0, 1).toUpperCase()
                + fieldName.substring(1);
    }

    /**
     * The get method of splicing an attribute
     *
     * @param fieldName
     * @return String
     */
    public static String parSetName(String fieldName) {
        if (null == fieldName || "".equals(fieldName)) {
            return null;
        }
        return "set" + fieldName.substring(0, 1).toUpperCase()
                + fieldName.substring(1);
    }

    /**
     * Determine whether there is a set method for an attribute
     *
     * @param methods
     * @param fieldSetMet
     * @return boolean
     */
    public static boolean checkSetMet(Method[] methods, String fieldSetMet) {
        for (Method met : methods) {
            if (fieldSetMet.equals(met.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine whether there is a get method for an attribute
     *
     * @param methods
     * @param fieldGetMet
     * @return boolean
     */
    public static boolean checkGetMet(Method[] methods, String fieldGetMet) {
        for (Method met : methods) {
            if (fieldGetMet.equals(met.getName())) {
                return true;
            }
        }
        return false;
    }
}
