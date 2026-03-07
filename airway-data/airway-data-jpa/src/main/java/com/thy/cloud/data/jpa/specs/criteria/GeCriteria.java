package com.thy.cloud.data.jpa.specs.criteria;

import jakarta.persistence.criteria.*;

public class GeCriteria<T> extends AbstractCriteria<T> {
    private final String property;
    private final transient Comparable<Object> compare;

    public GeCriteria(String property, Comparable<? extends Object> compare) {
        this.property = property;
        this.compare = (Comparable<Object>) compare;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        From from = getRoot(property, root);
        String field = getProperty(property);
        return cb.greaterThanOrEqualTo(from.get(field), compare);
    }
}
