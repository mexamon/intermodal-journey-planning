package com.thy.cloud.data.jpa.entity.support;

import java.util.List;

/**
 * Hierarchical Level Interface
 * @author Engin Mahmut
 *
 * @param <T>
 */
public interface Hierarchical<T> extends Sortable {

	T getParent();

	void setParent(T parent);

	List<T> getChildren();

	void setChildren(List<T> children);
}
