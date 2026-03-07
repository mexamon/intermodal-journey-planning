package com.thy.cloud.data.jpa.entity;

import com.thy.cloud.data.jpa.entity.support.Sortable;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.CompareToBuilder;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.io.Serializable;

/**
 * MappedSuperclass Sorting Entity for Tree Entities
 *
 * @author Engin Mahmut
 *
 * @param <U>
 * @param <I>
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractSortEntity<U, I extends Serializable> extends AbstractAuditionGuidKeyEntity
		implements Sortable, Comparable<AbstractSortEntity<U, I>> {

	private static final long serialVersionUID = -512099056914573545L;

	@Column(name = "sort_no")
	private Integer sortNo;

	@Override
	public Integer getSortNo() {
		return sortNo;
	}

	@Override
	public void setSortNo(Integer sortNo) {
		this.sortNo = sortNo;
	}

	@Override
	public int compareTo(AbstractSortEntity<U, I> o) {
		return new CompareToBuilder().append(getSortNo(), o.getSortNo()).append(getId(), o.getId()).toComparison();
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

}
