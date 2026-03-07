package com.thy.cloud.data.jpa.specs.criteria;

import org.springframework.data.domain.Example;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.util.Assert;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;


public class ExampleCriteria<T> extends AbstractCriteria<T> {

    private final Example<T> example;

    public ExampleCriteria(Example<T> example) {

        Assert.notNull(example, "Example must not be null!");
        this.example = example;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        return QueryByExamplePredicateBuilder.getPredicate(root, cb, example);
    }
}