package com.thy.cloud.data.jpa.specs.criteria;

import jakarta.persistence.criteria.*;

public class NotEqualCriteria<T> extends AbstractCriteria<T> {
    private final String property;
    private final transient Object[] values;

    public NotEqualCriteria(String property, Object... values) {
        this.property = property;
        this.values = values;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        From from = getRoot(property, root);
        String field = getProperty(property);
        if (values == null) {
            return cb.isNotNull(from.get(field));
        }
        if (values.length == 1) {
            return getPredicate(from, cb, values[0], field);
        }
        Predicate[] predicates = new Predicate[values.length];
        for (int i = 0; i < values.length; i++) {
            predicates[i] = getPredicate(root, cb, values[i], field);
        }
        return cb.or(predicates);
    }

    private Predicate getPredicate(From root, CriteriaBuilder cb, Object value, String field) {
        return value == null ? cb.isNotNull(root.get(field)) : cb.notEqual(root.get(field), value);
    }
}

