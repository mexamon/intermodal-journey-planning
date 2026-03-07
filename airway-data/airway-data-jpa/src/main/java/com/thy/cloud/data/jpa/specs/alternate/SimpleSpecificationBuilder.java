package com.thy.cloud.data.jpa.specs.alternate;

import java.util.ArrayList;
import java.util.List;

import com.thy.cloud.data.jpa.constant.PersistenceConstants;
import org.apache.commons.lang3.StringUtils;

import lombok. ToString;

/**
 * SimpleSpecification constructor
 *
 */
@ToString
public class SimpleSpecificationBuilder<T> {
	/** query operator set */
	private final List<SpecificationOperator> operators;

	/**
	 * Query specification constructor, the initialization condition is and
	 *
	 */
	public SimpleSpecificationBuilder(String key, Operator operator, Object value) {
		SpecificationOperator so = new SpecificationOperator();
		so.setJoin("and");
		so. setKey(key);
		so. setOperator(operator);
		so. setValue(value);
		operators = new ArrayList<>();
		operators. add(so);
	}
	/**
	 * query condition constructor
	 *
	 */
	public SimpleSpecificationBuilder() {
		operators = new ArrayList<>();
	}

	/**
	 * Complete the addition of conditions
	 *
	 * @return SimpleSpecificationBuilder
	 */
	public SimpleSpecificationBuilder<T> add(String join, String key, Operator operator, Object value) {
// Determine whether the query condition is valid
		boolean flag = !Operator.ISNULL.equals(operator) && !Operator.IS_NOTNULL.equals(operator)
				&& (null == value || StringUtils. isBlank(value. toString()));
		if (flag) {
			return this;
		}
		SpecificationOperator so = new SpecificationOperator();
		so. setJoin(join);
		so. setKey(key);
		so. setValue(value);
		so. setOperator(operator);
		operators. add(so);
		return this;
	}

	/**
	 * Add the condition of and
	 *
	 * @return SimpleSpecificationBuilder
	 */
	public SimpleSpecificationBuilder<T> and(String key, Operator operator, Object value) {
		return this.add("and", key, operator, value);
	}

	/**
	 * Add overload of or condition
	 *
	 * @return this, convenient for subsequent chain calls
	 */
	public SimpleSpecificationBuilder<T> or(String key, Operator operator, Object value) {
		return this.add("or", key, operator, value);
	}

	/**
	 * between
	 *
	 * @param key field name
	 * @param min decimal value
	 * @param max large value
	 * @return SimpleSpecificationBuilder
	 */
	public SimpleSpecificationBuilder<T> between(String key, Object min, Object max) {
		return between("and", key, min, max);
	}

	/**
	 * between
	 *
	 * @param join connector
	 * @param key field name
	 * @param min decimal value
	 * @param max large value
	 * @return SimpleSpecificationBuilder
	 */
	public SimpleSpecificationBuilder<T> between(String join, String key, Object min, Object max) {
		Object value1 = min;
		Object value2 = max;
		if (null == value1 || StringUtils. isBlank(value1. toString())) {
			return this.add(join, key, Operator.LE, value2);
		} else {
			value1 = value1 + "00:00:00";
		}
		if (null == value2 || StringUtils. isBlank(value2. toString())) {
			return this.add(join, key, Operator.GE, value1);
		} else {
			value2 = value2 + " 23:59:59";
		}
		return this.add(join, key, Operator.BETWEEN, value1 + PersistenceConstants.COMMA + value2);
	}

	/**
	 * query specification construction
	 *
	 * @return SimpleSpecification<T>
	 */
	public SimpleSpecification<T> build() {
		return new SimpleSpecification<>(this.operators);
	}
}