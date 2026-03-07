package com.thy.cloud.service.api.modules.policy.service;

import com.thy.cloud.service.api.modules.policy.model.PolicySetSearchRequest;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyConstraints;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyNode;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicySet;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyTransition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface PolicyService {

    // Policy Set
    Page<JourneyPolicySet> searchPolicySets(PolicySetSearchRequest request, Pageable pageable);

    JourneyPolicySet getPolicySet(UUID id);

    JourneyPolicySet getPolicySetByCode(String code);

    // Constraints
    JourneyPolicyConstraints getConstraints(UUID policySetId);

    // Nodes
    List<JourneyPolicyNode> listNodes(UUID policySetId);

    // Transitions
    List<JourneyPolicyTransition> listTransitions(UUID policySetId);
}
