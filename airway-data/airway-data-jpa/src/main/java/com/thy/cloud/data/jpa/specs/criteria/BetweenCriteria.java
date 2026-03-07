package com.thy.cloud.data.jpa.specs.criteria;


import jakarta.persistence.criteria.*;


public class BetweenCriteria<T> extends AbstractCriteria<T> {
    private final String property;
    private final transient Comparable<Object> lower;
    private final transient Comparable<Object> upper;

    public BetweenCriteria(String property, Object lower, Object upper) {
        this.property = property;
        this.lower = (Comparable<Object>) lower;
        this.upper = (Comparable<Object>) upper;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        From from = getRoot(property, root);
        String field = getProperty(property);
        return cb.between(from.get(field), lower, upper);
    }
}
