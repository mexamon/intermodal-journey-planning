package com.thy.cloud.data.jpa.specs.criteria;

import jakarta.persistence.criteria.*;
import java.util.Collection;

public class NotInCriteria<T> extends AbstractCriteria<T> {
    private final String property;
    private final transient Collection<?> values;

    public NotInCriteria(String property, Collection<?> values) {
        this.property = property;
        this.values = values;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        From from = getRoot(property, root);
        String field = getProperty(property);
        return from.get(field).in(values).not();
    }
}
