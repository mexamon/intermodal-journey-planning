package com.thy.cloud.data.jpa.specs;

import static jakarta.persistence.criteria.Predicate.BooleanOperator.AND;
import static jakarta.persistence.criteria.Predicate.BooleanOperator.OR;

/**
 * Specification Initializer
 *
 * @author Engin Mahmut
 */

public class Specifications {

    private Specifications() {
    }

    public static <T> PredicateBuilder<T> and() {
        return new PredicateBuilder<>(AND);
    }
    public static <T> PredicateBuilder<T> or() {
        return new PredicateBuilder<>(OR);
    }
}
