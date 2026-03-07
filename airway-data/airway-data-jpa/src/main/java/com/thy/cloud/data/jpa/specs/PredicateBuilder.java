package com.thy.cloud.data.jpa.specs;

import com.thy.cloud.data.jpa.entity.support.Range;
import com.thy.cloud.data.jpa.specs.criteria.*;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static jakarta.persistence.criteria.Predicate.BooleanOperator.OR;

/**
 * Predicate Builder Class for Operators
 *
 * @author Engin Mahmut
 */

public class PredicateBuilder<T> {

    private final Predicate.BooleanOperator operator;

    private List<Specification<T>> specifications;

    public PredicateBuilder(Predicate.BooleanOperator operator) {
        this.operator = operator;
        this.specifications = new ArrayList<>();
    }

    public PredicateBuilder<T> Equals(String property, Object... values) {
        return Equals(true, property, values);
    }

    public PredicateBuilder<T> Equals(boolean condition, String property, Object... values) {
        return this.predicate(condition, new EqualCriteria(property, values));
    }

    public PredicateBuilder<T> NotEquals(String property, Object... values) {
        return NotEquals(true, property, values);
    }

    public PredicateBuilder<T> NotEquals(boolean condition, String property, Object... values) {
        return this.predicate(condition, new NotEqualCriteria(property, values));
    }

    public PredicateBuilder<T> GreaterThan(String property, Comparable<?> compare) {
        return GreaterThan(true, property, compare);
    }

    public PredicateBuilder<T> GreaterThan(boolean condition, String property, Comparable<?> compare) {
        return this.predicate(condition, new GtCriteria(property, compare));
    }

    public PredicateBuilder<T> GreaterThanEqualTo(String property, Comparable<?> compare) {
        return GreaterThanEqualTo(true, property, compare);
    }

    public PredicateBuilder<T> GreaterThanEqualTo(boolean condition, String property, Comparable<? extends Object> compare) {
        return this.predicate(condition, new GeCriteria(property, compare));
    }

    public PredicateBuilder<T> LessThan(String property, Comparable<?> number) {
        return LessThan(true, property, number);
    }

    public PredicateBuilder<T> LessThan(boolean condition, String property, Comparable<?> compare) {
        return this.predicate(condition, new LtCriteria(property, compare));
    }

    public PredicateBuilder<T> LessThanEqualTo(String property, Comparable<?> compare) {
        return LessThanEqualTo(true, property, compare);
    }

    public PredicateBuilder<T> LessThanEqualTo(boolean condition, String property, Comparable<?> compare) {
        return this.predicate(condition, new LeCriteria(property, compare));
    }

    public PredicateBuilder<T> Between(String property, Object lower, Object upper) {
        return Between(true, property, lower, upper);
    }

    public PredicateBuilder<T> Between(boolean condition, String property, Object lower, Object upper) {
        return this.predicate(condition, new BetweenCriteria(property, lower, upper));
    }

    public PredicateBuilder<T> Like(String property, String... patterns) {
        return Like(true, property, patterns);
    }

    public PredicateBuilder<T> Like(boolean condition, String property, String... patterns) {
        return this.predicate(condition, new LikeCriteria(property, patterns));
    }

    public PredicateBuilder<T> NotLike(String property, String... patterns) {
        return NotLike(true, property, patterns);
    }

    public PredicateBuilder<T> NotLike(boolean condition, String property, String... patterns) {
        return this.predicate(condition, new NotLikeCriteria(property, patterns));
    }

    public PredicateBuilder<T> In(String property, Collection<?> values) {
        return this.In(true, property, values);
    }

    public PredicateBuilder<T> In(boolean condition, String property, Collection<?> values) {
        return this.predicate(condition, new InCriteria(property, values));
    }

    public PredicateBuilder<T> NotIn(String property, Collection<?> values) {
        return this.NotIn(true, property, values);
    }

    public PredicateBuilder<T> NotIn(boolean condition, String property, Collection<?> values) {
        return this.predicate(condition, new NotInCriteria(property, values));
    }

    public PredicateBuilder<T> predicate(Specification specification) {
        return predicate(true, specification);
    }

    public PredicateBuilder<T> predicate(boolean condition, Specification specification) {
        if (condition) {
            this.specifications.add(specification);
        }
        return this;
    }

    public PredicateBuilder<T> range(List<Range<T>> range) {
        return this.range(true, range);
    }

    public PredicateBuilder<T> range(boolean condition, List<Range<T>> range) {
        return this.predicate(condition, new RangeCriteria(range));
    }

    public PredicateBuilder<T> Example(Example<T> example) {
        return this.Example(true, example);
    }

    public PredicateBuilder<T> Example(boolean condition, Example<T> example) {
        return this.predicate(condition, new ExampleCriteria(example));
    }

    public Specification<T> build() {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate[] predicates = specifications.stream().map(specification -> specification.toPredicate(root, query, cb)).toArray(Predicate[]::new);
            if (Objects.equals(predicates.length, 0)) {
                return null;
            }
            return OR.equals(operator) ? cb.or(predicates) : cb.and(predicates);
        };
    }
}
