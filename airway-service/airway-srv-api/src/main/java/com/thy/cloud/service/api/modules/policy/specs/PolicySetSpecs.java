package com.thy.cloud.service.api.modules.policy.specs;

import com.thy.cloud.service.api.modules.policy.model.PolicySetSearchRequest;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicySet;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicySet_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.ObjectUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class PolicySetSpecs {

    private PolicySetSpecs() {
    }

    public static Specification<JourneyPolicySet> filter(PolicySetSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!ObjectUtils.isEmpty(request.getCode())) {
                predicates.add(cb.like(cb.lower(root.get(JourneyPolicySet_.code)),
                        "%" + request.getCode().toLowerCase() + "%"));
            }

            if (request.getScopeType() != null) {
                predicates.add(cb.equal(root.get(JourneyPolicySet_.scopeType), request.getScopeType()));
            }

            if (!ObjectUtils.isEmpty(request.getScopeKey())) {
                predicates.add(cb.equal(root.get(JourneyPolicySet_.scopeKey), request.getScopeKey()));
            }

            if (request.getSegment() != null) {
                predicates.add(cb.equal(root.get(JourneyPolicySet_.segment), request.getSegment()));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get(JourneyPolicySet_.status), request.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
