package com.thy.cloud.data.jpa.util;

import com.thy.cloud.data.jpa.entity.AbstractTreeEntity;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.io.Serializable;

/**
 * Tree Children Query Specification
 * @author Engin Mahmut
 *
 * @param <T>
 */
public class TreeChildrenSpecification<T extends AbstractTreeEntity<?, I, T>, I extends Serializable> implements Specification<T> {

	private final T current;

	private final String treePathProperty;

	public TreeChildrenSpecification(T current, String treePathProperty) {
		this.current = current;
		this.treePathProperty = treePathProperty;
	}

	@Override
	public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

		/*StringBuilder rootPath = new StringBuilder();

		if (!ObjectUtils.isEmpty(current.getTreePath())){
			rootPath.append(current.getTreePath());
		}
		rootPath.append(current.getId());*/

		String rootPath = current.getTreePath() + current.getId();
		return cb.like(root.get(treePathProperty), rootPath + "%");
	}

}
