package com.thy.cloud.data.jpa.specs.criteria;

import com.thy.cloud.data.jpa.entity.support.Range;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class RangeCriteria <T> extends AbstractCriteria<T> {

    private final List<Range<T>> ranges;

    public  RangeCriteria(List<Range<T>> ranges) {
        this.ranges = ranges;
    }

    @Override
    public  Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

        List<Predicate> predicates = new ArrayList<>();

        ranges.stream().filter(Range::isSet).forEach(range -> {
            Predicate rangePredicate = getPredicate(range, root, cb);
            if (rangePredicate != null) {
                if (!range.isIncludeNullSet() || range.getIncludeNull().equals(FALSE)) {
                    predicates.add(rangePredicate);
                } else {
                    predicates.add(cb.or(rangePredicate, cb.isNull(root.get(range.getField()))));
                }
            } else {
                if (TRUE.equals(range.getIncludeNull())) {
                    predicates.add(cb.isNull(root.get(range.getField())));
                } else if (FALSE.equals(range.getIncludeNull())) {
                    predicates.add(cb.isNotNull(root.get(range.getField())));
                }
            }
        });
        return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
    }

    private Predicate getPredicate(Range<T> range, Root<T> root, CriteriaBuilder cb) {
        if (range.isBetween()) {
            return cb.between(root.get(range.getField()), range.getFrom(), range.getTo());
        } else if (range.isFromSet()) {
            return cb.greaterThanOrEqualTo(root.get(range.getField()), range.getFrom());
        } else if (range.isToSet()) {
            return cb.lessThanOrEqualTo(root.get(range.getField()), range.getTo());
        }
        return null;
    }


}
