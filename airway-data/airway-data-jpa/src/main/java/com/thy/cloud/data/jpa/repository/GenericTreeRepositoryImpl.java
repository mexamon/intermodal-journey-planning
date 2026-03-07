package com.thy.cloud.data.jpa.repository;

import com.thy.cloud.data.jpa.entity.AbstractTreeEntity;
import com.thy.cloud.data.jpa.entity.support.Node;
import com.thy.cloud.data.jpa.entity.support.Tree;
import com.thy.cloud.data.jpa.entity.support.TreeHelper;
import com.thy.cloud.data.jpa.util.TreeChildrenSpecification;
import com.thy.cloud.data.jpa.util.TreeParentSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.repository.NoRepositoryBean;

import jakarta.persistence.EntityManager;
import java.io.Serializable;
import java.util.List;

/**
 * Generic Tree Repository Implementation
 * @author Engin Mahmut
 *
 * @param <T>
 * @param <I>
 */
@NoRepositoryBean
public class GenericTreeRepositoryImpl
		<T extends AbstractTreeEntity<?, I, T>, I extends Serializable>
		extends GenericRepositoryImpl<T, I>
		implements
		GenericTreeRepository<T, I>
 {

	public GenericTreeRepositoryImpl(JpaEntityInformation<T, I> entityInformation, EntityManager entityManager) {
		super(entityInformation, entityManager);
	}

	@Override
	public List<T> findRoots() {
		return getQuery(new TreeParentSpecification<>(null, "parent"), Sort.by("id")).getResultList();
	}

	@Override
	public List<T> findAllChildren(T root) {
		return getQuery(new TreeChildrenSpecification<>(root, "treePath"),  Sort.by("id")).getResultList();
	}

	@Override
	public Tree<T> findByRoot(T root) {
		List<T> allChildren = root == null ? findAll() : findAllChildren(root);
		return TreeHelper.toTree(root, allChildren);
	}

	@Override
	public Tree<T> findTree(Specification<T> predicate) {
		return findTree(predicate, null);
	}

	@Override
	public Tree<T> findTree(Specification<T> predicate, Node<T> singleRoot) {
		List<T> allChildren = findAll(predicate);
		return TreeHelper.toTree(null, allChildren, singleRoot);
	}

	@Override
	public T sort(T source, T target, String action) {
		switch (action) {
			case "over":
				source.setParent(target);
				return save(source);
			case "before":
			case "after":
				Integer sourceSortNo = source.getSortNo();
				Integer targetSortNo = target.getSortNo();

				T parent = target.getParent();

				source.setSortNo(targetSortNo);
				source.setParent(parent);
				target.setSortNo(sourceSortNo);
				save(target);
				return save(source);
			default:
				break;
		}

		return source;
	}
}
