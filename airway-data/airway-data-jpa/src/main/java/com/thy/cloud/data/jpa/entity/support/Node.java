package com.thy.cloud.data.jpa.entity.support;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Node Details Class
 * @author Engin Mahmut
 *
 * @param <T>
 */
@Getter
@Setter
public class Node<T extends Hierarchical<T>> {

	private T data;
	private List<Node<T>> children;

	private Boolean checked;
	private Boolean expanded;

	private String text;

	private String iconCls;

	public boolean getLeaf() {
		List<Node<T>> childrenList = getChildren();
		return childrenList == null || childrenList.isEmpty();
	}

}
