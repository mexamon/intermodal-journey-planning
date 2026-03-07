package com.thy.cloud.data.jpa.entity.listener;

import com.thy.cloud.data.jpa.entity.AbstractTreeEntity;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.io.Serializable;

/**
 *
 * <h2>HierarchicalEntityListener</h2>
 *
 * @author Engin Mahmut
 *
 * Tree Path Organizer Listener
 *
 */
public class TreeEntityListener {

	/** Tree path separator */
	private static final String TREE_PATH_SEPARATOR = ",";

	/**
	 *
	 * @param entity Entity Class
	 * @param <U>
	 * @param <I>
	 * @param <T>
	 */
	@PrePersist
	public <U, I extends Serializable, T> void prePersist(AbstractTreeEntity<U, I, T> entity) {
		@SuppressWarnings("unchecked")
		AbstractTreeEntity<U, I, T> parent = (AbstractTreeEntity<U, I, T>) entity.getParent();
		if (parent != null) {
			entity.setTreePath(parent.getTreePath() + parent.getId() + TREE_PATH_SEPARATOR);
		} else {
			entity.setTreePath(TREE_PATH_SEPARATOR);
		}
	}

	/**
	 *
	 * @param entity Entity Class
	 * @param <U>
	 * @param <I>
	 * @param <T>
	 */
	@PreUpdate
	public <U, I extends Serializable, T> void preUpdate(AbstractTreeEntity<U, I, T> entity) {
		@SuppressWarnings("unchecked")
		AbstractTreeEntity<U, I, T> parent = (AbstractTreeEntity<U, I, T>) entity.getParent();
		if (parent != null) {
			entity.setTreePath(parent.getTreePath() + parent.getId() + TREE_PATH_SEPARATOR);
		} else {
			entity.setTreePath(TREE_PATH_SEPARATOR);
		}
	}
}
