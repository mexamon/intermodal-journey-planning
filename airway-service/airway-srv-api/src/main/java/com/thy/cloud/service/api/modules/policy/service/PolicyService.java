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

    // Policy Set — Read
    Page<JourneyPolicySet> searchPolicySets(PolicySetSearchRequest request, Pageable pageable);

    JourneyPolicySet getPolicySet(UUID id);

    JourneyPolicySet getPolicySetByCode(String code);

    // Policy Set — Write
    JourneyPolicySet createPolicySet(JourneyPolicySet policySet);

    JourneyPolicySet updatePolicySet(UUID id, JourneyPolicySet policySet);

    void deletePolicySet(UUID id);

    // Constraints
    JourneyPolicyConstraints getConstraints(UUID policySetId);

    JourneyPolicyConstraints saveConstraints(UUID policySetId, JourneyPolicyConstraints constraints);

    // Nodes
    List<JourneyPolicyNode> listNodes(UUID policySetId);

    List<JourneyPolicyNode> saveNodes(UUID policySetId, List<JourneyPolicyNode> nodes);

    // Transitions
    List<JourneyPolicyTransition> listTransitions(UUID policySetId);

    List<JourneyPolicyTransition> saveTransitions(UUID policySetId, List<JourneyPolicyTransition> transitions);
}
