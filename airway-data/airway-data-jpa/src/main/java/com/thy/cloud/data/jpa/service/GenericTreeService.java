package com.thy.cloud.data.jpa.service;

import com.thy.cloud.data.jpa.entity.AbstractTreeEntity;
import com.thy.cloud.data.jpa.entity.support.Node;
import com.thy.cloud.data.jpa.entity.support.Tree;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;
import java.util.List;

/**
 * Generic Tree Services Interface
 *
 * @author Engin Mahmut
 */

public interface GenericTreeService<T extends AbstractTreeEntity<?, I, T>, I extends Serializable>
extends GenericService<T, I>{

    List<T> findRoots();

    List<T> findAllChildren(T root);

    Tree<T> findByRoot(T root);

    Tree<T> findTree(Specification<T> predicate);

    Tree<T> findTree(Specification<T> predicate, Node<T> singleRoot);

    T sort(T source, T target, String action);

}
