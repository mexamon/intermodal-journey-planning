package com.thy.cloud.data.jpa.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thy.cloud.data.jpa.entity.listener.TreeEntityListener;
import com.thy.cloud.data.jpa.entity.support.Hierarchical;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.List;

/**
 * MappedSuperclass that contains all the necessary fields for Tree Entities
 *
 * @author Engin Mahmut
 *
 * @param <U>
 * @param <ID>
 * @param <T>
 */
@MappedSuperclass
@EntityListeners(value = { TreeEntityListener.class })
public abstract class AbstractTreeEntity<U, ID extends Serializable, T> extends AbstractSortEntity<U, ID>
		implements Hierarchical<T> {

	private static final long serialVersionUID = 4795899175741576611L;

	@JsonIgnore
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_ref")
	private transient T parent;

	@JsonIgnore
	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE })
	private List<T> children;

	@Getter
	@Setter
	@JsonIgnore
	@Column(name = "tree_path", length = 500)
	private String treePath;

	@Override
	public T getParent() {
		return parent;
	}

	@Override
	public void setParent(T parent) {
		this.parent = parent;
	}

	@Override
	public List<T> getChildren() {
		return children;
	}

	@Override
	public void setChildren(List<T> children) {
		this.children = children;
	}

	public boolean isLeaf() {
		return children == null || children.isEmpty();
	}

	public boolean isRoot() {
		return parent == null;
	}

}
