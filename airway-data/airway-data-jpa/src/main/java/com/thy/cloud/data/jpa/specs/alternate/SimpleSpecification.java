package com.thy.cloud.data.jpa.specs.alternate;

import java.util.Date;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import com.thy.cloud.base.util.date.DateUtil;
import com.thy.cloud.data.jpa.constant.PersistenceConstants;
import org.springframework.data.jpa.domain.Specification;

import lombok.ToString;

/**
 * Package Specification tool class
 *
 */
@ToString
public class SimpleSpecification<T> implements Specification<T> {
	/** serialVersionUID */
	private static final long serialVersionUID = -1012947070278956018L;
	/**
	 * The condition list of the query, which is a set of lists
	 */
	private final List<SpecificationOperator> operators;

	/**
	 * constructor
	 *
	 * @param operators SpecificationOperator set
	 */
	public SimpleSpecification(List<SpecificationOperator> operators) {
		this.operators = operators;
	}

	@Override
	public Predicate toPredicate(Root<T> root, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
		// Combine multiple conditions with resultPre
		Predicate resultPre = criteriaBuilder.conjunction();
		for (SpecificationOperator op : operators) {
			Predicate pre = generatePredicate(root, criteriaBuilder, op);
			if (pre == null) {
				continue;
			}
			if ("and".equalsIgnoreCase(op.getJoin())) {
				resultPre = criteriaBuilder.and(resultPre, pre);
			} else if ("or".equalsIgnoreCase(op.getJoin())) {
				resultPre = criteriaBuilder.or(resultPre, pre);
			}
		}
		return criteriaQuery.where(resultPre).getRestriction();
	}

	/**
	 * Return specific queries based on different operators
	 *
	 * @param root            Root<T>
	 * @param criteriaBuilder CriteriaBuilder
	 * @param op              SpecificationOperator
	 * @return Predicate
	 */
	private Predicate generatePredicate(Root<T> root, CriteriaBuilder criteriaBuilder, SpecificationOperator op) {
		switch (op.getOperator()) {
			case EQUAL:
				return criteriaBuilder.equal(root.get(op.getKey()), op.getValue());
			case NOT_EQUAL:
				return criteriaBuilder.notEqual(root.get(op.getKey()), op.getValue());
			case GE:
				return criteriaBuilder.ge(root.get(op.getKey()), (Number) op.getValue());
			case LE:
				return criteriaBuilder.le(root.get(op.getKey()), (Number) op.getValue());
			case GT:
				return criteriaBuilder.gt(root.get(op.getKey()), (Number) op.getValue());
			case LT:
				return criteriaBuilder.lt(root.get(op.getKey()), (Number) op.getValue());
			case LIKE:
				return criteriaBuilder.like(root.get(op.getKey()),
						PersistenceConstants.PERCENT_SIGN + op.getValue() + PersistenceConstants.PERCENT_SIGN);
			case R_LIKE:
				return criteriaBuilder.like(root.get(op.getKey()),
						op.getValue() + PersistenceConstants.PERCENT_SIGN);
			case L_LIKE:
				return criteriaBuilder.like(root.get(op.getKey()),
						PersistenceConstants.PERCENT_SIGN + op.getValue());
			case ISNULL:
				return criteriaBuilder.isNull(root.get(op.getKey()));
			case IS_NOTNULL:
				return criteriaBuilder.isNotNull(root.get(op.getKey()));
			case BETWEEN:
				String[] arr = op.getValue().toString().split(PersistenceConstants.COMMA);
				Date beginTime = DateUtil.parseDate(arr[0]);
				Date endTime = DateUtil.parseDate(arr[1]);
				return criteriaBuilder.between(root.get(op.getKey()), beginTime, endTime);
			default:
				break;
		}
		return null;
	}

}