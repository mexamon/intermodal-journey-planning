package com.thy.cloud.data.jpa.entity.support;

import com.google.common.base.MoreObjects;

import java.util.Comparator;

/**
 * Sort Number Comparator
 * @author Engin Mahmut
 * 
 * @param <T>
 */
public class SortNoComparator<T extends Sortable> implements Comparator<T> {

	public static final SortNoComparator<Sortable> COMPARATOR = new SortNoComparator<>();

	@Override
	public int compare(T o1, T o2) {
		Integer sortNo1 = MoreObjects.firstNonNull(o1.getSortNo(), 0);
		Integer sortNo2 = MoreObjects.firstNonNull(o2.getSortNo(), 0);
		return sortNo1 - sortNo2;
	}

}
