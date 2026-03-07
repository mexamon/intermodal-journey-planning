package com.thy.cloud.data.jpa.repository;

import com.thy.cloud.data.jpa.entity.AbstractTreeEntity;
import com.thy.cloud.data.jpa.entity.support.Node;
import com.thy.cloud.data.jpa.entity.support.Tree;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

/**
 * Generic Tree Repository Interface
 * @author Engin Mahmut
 * @param <T>
 * @param <I>
 */
@NoRepositoryBean
public interface GenericTreeRepository
		<T extends AbstractTreeEntity<?, I, T>, I extends Serializable>
		extends
		GenericRepository<T, I> {

	/**
	 *
	 * @return
	 */
	List<T> findRoots();

	/**
	 *
	 * @param root
	 * @return
	 */
	List<T> findAllChildren(T root);

	/**
	 *
	 * @param root
	 * @return
	 */
	Tree<T> findByRoot(T root);

	/**
	 *
	 * @param predicate
	 * @return
	 */
	Tree<T> findTree(Specification<T> predicate);

	/**
	 *
	 * @param predicate
	 * @param singleRoot
	 * @return
	 */
	Tree<T> findTree(Specification<T> predicate, Node<T> singleRoot);

	/**
	 *
	 * @param source
	 * @param target
	 * @param action
	 * @return
	 */
	T sort(T source, T target, String action);
}
