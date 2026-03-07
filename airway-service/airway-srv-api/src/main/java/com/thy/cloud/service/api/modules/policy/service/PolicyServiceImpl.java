package com.thy.cloud.service.api.modules.policy.service;

import com.thy.cloud.service.api.modules.policy.model.PolicySetSearchRequest;
import com.thy.cloud.service.api.modules.policy.specs.PolicySetSpecs;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyConstraints;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyNode;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicySet;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyTransition;
import com.thy.cloud.service.dao.repository.policy.JourneyPolicyConstraintsRepository;
import com.thy.cloud.service.dao.repository.policy.JourneyPolicyNodeRepository;
import com.thy.cloud.service.dao.repository.policy.JourneyPolicySetRepository;
import com.thy.cloud.service.dao.repository.policy.JourneyPolicyTransitionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private final JourneyPolicySetRepository policySetRepository;
    private final JourneyPolicyConstraintsRepository constraintsRepository;
    private final JourneyPolicyNodeRepository nodeRepository;
    private final JourneyPolicyTransitionRepository transitionRepository;

    // ── Policy Set ────────────────────────────────────────────

    @Override
    public Page<JourneyPolicySet> searchPolicySets(PolicySetSearchRequest request, Pageable pageable) {
        return policySetRepository.findAll(PolicySetSpecs.filter(request), pageable);
    }

    @Override
    public JourneyPolicySet getPolicySet(UUID id) {
        return policySetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Policy set not found: " + id));
    }

    @Override
    public JourneyPolicySet getPolicySetByCode(String code) {
        return policySetRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Policy set not found for code: " + code));
    }

    // ── Constraints ───────────────────────────────────────────

    @Override
    public JourneyPolicyConstraints getConstraints(UUID policySetId) {
        return constraintsRepository.findById(policySetId)
                .orElseThrow(() -> new EntityNotFoundException("Constraints not found for policy set: " + policySetId));
    }

    // ── Nodes ─────────────────────────────────────────────────

    @Override
    public List<JourneyPolicyNode> listNodes(UUID policySetId) {
        return nodeRepository.findByPolicySetId(policySetId);
    }

    // ── Transitions ───────────────────────────────────────────

    @Override
    public List<JourneyPolicyTransition> listTransitions(UUID policySetId) {
        return transitionRepository.findByPolicySetId(policySetId);
    }
}
