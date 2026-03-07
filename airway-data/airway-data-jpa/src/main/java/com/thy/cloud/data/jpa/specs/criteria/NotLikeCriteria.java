package com.thy.cloud.data.jpa.specs.criteria;

import jakarta.persistence.criteria.*;

public class NotLikeCriteria<T> extends AbstractCriteria<T> {
    private final String property;
    private final String[] patterns;

    public NotLikeCriteria(String property, String... patterns) {
        this.property = property;
        this.patterns = patterns;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        From from = getRoot(property, root);
        String field = getProperty(property);
        if (patterns.length == 1) {
            return cb.like(from.get(field), patterns[0]).not();
        }
        Predicate[] predicates = new Predicate[patterns.length];
        for (int i = 0; i < patterns.length; i++) {
            predicates[i] = cb.like(from.get(field), patterns[i]).not();
        }
        return cb.or(predicates);
    }
}
