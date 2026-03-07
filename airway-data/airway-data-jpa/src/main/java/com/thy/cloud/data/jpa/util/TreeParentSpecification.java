package com.thy.cloud.data.jpa.util;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Tree Parent Filter Specification
 * @author Engin Mahmut
 *
 * @param <T>
 */
public class TreeParentSpecification<T> implements Specification<T> {

	private final transient T parent;

	private final String parentProperty;

	public TreeParentSpecification(T parent, String parentProperty) {
		this.parent = parent;
		this.parentProperty = parentProperty;
	}

	@Override
	public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
		Predicate predicate;
		if (parent == null)
			predicate = cb.isNull(root.get(parentProperty));
		else
			predicate = cb.equal(root.get(parentProperty), parent);
		return predicate;
	}


}
