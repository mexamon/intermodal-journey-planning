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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private final JourneyPolicySetRepository policySetRepository;
    private final JourneyPolicyConstraintsRepository constraintsRepository;
    private final JourneyPolicyNodeRepository nodeRepository;
    private final JourneyPolicyTransitionRepository transitionRepository;

    // ── Policy Set — Read ─────────────────────────────────────

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

    // ── Policy Set — Write ────────────────────────────────────

    @Override
    @Transactional
    public JourneyPolicySet createPolicySet(JourneyPolicySet policySet) {
        if (policySet.getCode() == null || policySet.getCode().isBlank()) {
            throw new IllegalArgumentException("Policy set code is required");
        }
        policySetRepository.findByCode(policySet.getCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("Policy set code already exists: " + policySet.getCode());
        });
        if (policySet.getScopeType() == null) {
            throw new IllegalArgumentException("Scope type is required");
        }
        if (policySet.getScopeKey() == null || policySet.getScopeKey().isBlank()) {
            throw new IllegalArgumentException("Scope key is required");
        }
        log.info("Creating policy set: code={}, scope={}/{}", policySet.getCode(),
                policySet.getScopeType(), policySet.getScopeKey());
        return policySetRepository.save(policySet);
    }

    @Override
    @Transactional
    public JourneyPolicySet updatePolicySet(UUID id, JourneyPolicySet update) {
        JourneyPolicySet existing = getPolicySet(id);
        if (!existing.getCode().equals(update.getCode())) {
            policySetRepository.findByCode(update.getCode()).ifPresent(dup -> {
                throw new IllegalArgumentException("Policy set code already exists: " + update.getCode());
            });
        }
        existing.setCode(update.getCode());
        existing.setScopeType(update.getScopeType());
        existing.setScopeKey(update.getScopeKey());
        existing.setSegment(update.getSegment());
        existing.setStatus(update.getStatus());
        existing.setDescription(update.getDescription());
        existing.setEffectiveFrom(update.getEffectiveFrom());
        existing.setEffectiveTo(update.getEffectiveTo());
        log.info("Updated policy set: id={}, code={}", id, existing.getCode());
        return policySetRepository.save(existing);
    }

    @Override
    @Transactional
    public void deletePolicySet(UUID id) {
        JourneyPolicySet existing = getPolicySet(id);
        transitionRepository.deleteAll(transitionRepository.findByPolicySetId(id));
        nodeRepository.deleteAll(nodeRepository.findByPolicySetId(id));
        constraintsRepository.findById(id).ifPresent(constraintsRepository::delete);
        policySetRepository.delete(existing);
        log.info("Deleted policy set: id={}, code={}", id, existing.getCode());
    }

    // ── Constraints ───────────────────────────────────────────

    @Override
    public JourneyPolicyConstraints getConstraints(UUID policySetId) {
        return constraintsRepository.findById(policySetId)
                .orElseThrow(() -> new EntityNotFoundException("Constraints not found for policy set: " + policySetId));
    }

    @Override
    @Transactional
    public JourneyPolicyConstraints saveConstraints(UUID policySetId, JourneyPolicyConstraints constraints) {
        if (constraints.getMaxLegs() != null && constraints.getMaxLegs() < 1) {
            throw new IllegalArgumentException("Max legs must be >= 1");
        }
        if (constraints.getMaxTransfers() != null && constraints.getMaxTransfers() < 0) {
            throw new IllegalArgumentException("Max transfers must be >= 0");
        }
        if (constraints.getMinFlights() != null && constraints.getMaxFlights() != null
                && constraints.getMinFlights() > constraints.getMaxFlights()) {
            throw new IllegalArgumentException("Min flights cannot exceed max flights");
        }
        getPolicySet(policySetId);
        constraints.setId(policySetId);
        log.info("Saving constraints for policy set: {}", policySetId);
        return constraintsRepository.save(constraints);
    }

    // ── Nodes ─────────────────────────────────────────────────

    @Override
    public List<JourneyPolicyNode> listNodes(UUID policySetId) {
        return nodeRepository.findByPolicySetId(policySetId);
    }

    @Override
    @Transactional
    public List<JourneyPolicyNode> saveNodes(UUID policySetId, List<JourneyPolicyNode> nodes) {
        JourneyPolicySet policySet = getPolicySet(policySetId);
        nodeRepository.deleteAll(nodeRepository.findByPolicySetId(policySetId));
        nodeRepository.flush();
        for (JourneyPolicyNode node : nodes) {
            node.setPolicySet(policySet);
            if (node.getId() == null) node.setId(UUID.randomUUID());
        }
        log.info("Saving {} nodes for policy set: {}", nodes.size(), policySetId);
        return nodeRepository.saveAll(nodes);
    }

    // ── Transitions ───────────────────────────────────────────

    @Override
    public List<JourneyPolicyTransition> listTransitions(UUID policySetId) {
        return transitionRepository.findByPolicySetId(policySetId);
    }

    @Override
    @Transactional
    public List<JourneyPolicyTransition> saveTransitions(UUID policySetId, List<JourneyPolicyTransition> transitions) {
        JourneyPolicySet policySet = getPolicySet(policySetId);
        transitionRepository.deleteAll(transitionRepository.findByPolicySetId(policySetId));
        transitionRepository.flush();
        for (JourneyPolicyTransition t : transitions) {
            t.setPolicySet(policySet);
            if (t.getId() == null) t.setId(UUID.randomUUID());
        }
        log.info("Saving {} transitions for policy set: {}", transitions.size(), policySetId);
        return transitionRepository.saveAll(transitions);
    }
}
