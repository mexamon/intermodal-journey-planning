package com.thy.cloud.data.jpa.entity.support;

/**
 * Sort in ascending order according to the size of sortNo by default
 * 
 * @author Engin Mahmut
 */
public interface Sortable {
	/**
	 * Get Sorting Number
	 * @return integer
	 */
	Integer getSortNo();

	/**
	 * Set Sorting Number
	 * @param sortNo sorting number
	 */
	void setSortNo(Integer sortNo);
}
