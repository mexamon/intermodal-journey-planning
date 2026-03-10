package com.thy.cloud.service.api.modules.policy.service;

import com.thy.cloud.service.api.modules.policy.model.*;
import com.thy.cloud.service.api.modules.policy.specs.PolicySetSpecs;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyConstraints;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyNode;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicySet;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyTransition;
import com.thy.cloud.service.dao.enums.EnumNodeKey;
import com.thy.cloud.service.dao.enums.EnumPolicyScopeType;
import com.thy.cloud.service.dao.enums.EnumPolicySegment;
import com.thy.cloud.service.dao.enums.EnumPolicyStatus;
import com.thy.cloud.service.dao.repository.policy.JourneyPolicyConstraintsRepository;
import com.thy.cloud.service.dao.repository.policy.JourneyPolicyNodeRepository;
import com.thy.cloud.service.dao.repository.policy.JourneyPolicySetRepository;
import com.thy.cloud.service.dao.repository.policy.JourneyPolicyTransitionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @CacheEvict(value = PolicyCacheKey.CACHE_NAME, allEntries = true)
    public JourneyPolicySet createPolicySet(PolicySetRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("Policy set code is required");
        }
        policySetRepository.findByCode(request.getCode()).ifPresent(existing -> {
            throw new IllegalArgumentException("Policy set code already exists: " + request.getCode());
        });
        if (request.getScopeType() == null || request.getScopeType().isBlank()) {
            throw new IllegalArgumentException("Scope type is required");
        }
        if (request.getScopeKey() == null || request.getScopeKey().isBlank()) {
            throw new IllegalArgumentException("Scope key is required");
        }

        JourneyPolicySet entity = new JourneyPolicySet();
        entity.setCode(request.getCode().trim().toUpperCase());
        entity.setScopeType(EnumPolicyScopeType.valueOf(request.getScopeType()));
        entity.setScopeKey(request.getScopeKey());
        entity.setSegment(EnumPolicySegment.valueOf(
                request.getSegment() != null ? request.getSegment() : "DEFAULT"));
        entity.setStatus(EnumPolicyStatus.valueOf(
                request.getStatus() != null ? request.getStatus() : "DRAFT"));
        entity.setDescription(request.getDescription());""
        entity.setEffectiveFrom(request.getEffectiveFrom());
        entity.setEffectiveTo(request.getEffectiveTo());

        log.info("Creating policy set: code={}, scope={}/{}", entity.getCode(),
                entity.getScopeType(), entity.getScopeKey());
        return policySetRepository.save(entity);
    }

    @Override
    @Transactional
    @CacheEvict(value = PolicyCacheKey.CACHE_NAME, allEntries = true)
    public JourneyPolicySet updatePolicySet(UUID id, PolicySetRequest request) {
        JourneyPolicySet existing = getPolicySet(id);
        if (request.getCode() != null && !existing.getCode().equals(request.getCode())) {
            policySetRepository.findByCode(request.getCode()).ifPresent(dup -> {
                throw new IllegalArgumentException("Policy set code already exists: " + request.getCode());
            });
            existing.setCode(request.getCode().trim().toUpperCase());
        }
        if (request.getScopeType() != null) {
            existing.setScopeType(EnumPolicyScopeType.valueOf(request.getScopeType()));
        }
        if (request.getScopeKey() != null) {
            existing.setScopeKey(request.getScopeKey());
        }
        if (request.getSegment() != null) {
            existing.setSegment(EnumPolicySegment.valueOf(request.getSegment()));
        }
        if (request.getStatus() != null) {
            existing.setStatus(EnumPolicyStatus.valueOf(request.getStatus()));
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        existing.setEffectiveFrom(request.getEffectiveFrom());
        existing.setEffectiveTo(request.getEffectiveTo());

        log.info("Updated policy set: id={}, code={}", id, existing.getCode());
        return policySetRepository.save(existing);
    }

    @Override
    @Transactional
    @CacheEvict(value = PolicyCacheKey.CACHE_NAME, allEntries = true)
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
        return constraintsRepository.findByPolicySetId(policySetId).orElse(null);
    }

    @Override
    @Transactional
    @CacheEvict(value = PolicyCacheKey.CACHE_NAME, allEntries = true)
    public JourneyPolicyConstraints saveConstraints(UUID policySetId, ConstraintsRequest request) {
        if (request.getMaxLegs() != null && request.getMaxLegs() < 1) {
            throw new IllegalArgumentException("Max legs must be >= 1");
        }
        if (request.getMaxTransfers() != null && request.getMaxTransfers() < 0) {
            throw new IllegalArgumentException("Max transfers must be >= 0");
        }
        if (request.getMinFlights() != null && request.getMaxFlights() != null
                && request.getMinFlights() > request.getMaxFlights()) {
            throw new IllegalArgumentException("Min flights cannot exceed max flights");
        }
        JourneyPolicySet policySet = getPolicySet(policySetId);

        JourneyPolicyConstraints entity = constraintsRepository.findByPolicySetId(policySetId)
                .orElse(new JourneyPolicyConstraints());
        if (entity.getId() == null) {
            entity.setId(policySetId);
        }
        entity.setPolicySet(policySet);
        entity.setMaxLegs(request.getMaxLegs() != null ? request.getMaxLegs() : 5);
        entity.setMinFlights(request.getMinFlights() != null ? request.getMinFlights() : 1);
        entity.setMaxFlights(request.getMaxFlights() != null ? request.getMaxFlights() : 2);
        entity.setMinTransfers(request.getMinTransfers() != null ? request.getMinTransfers() : 0);
        entity.setMaxTransfers(request.getMaxTransfers() != null ? request.getMaxTransfers() : 2);
        entity.setMaxTotalDurationMin(request.getMaxTotalDurationMin());
        entity.setMaxWalkingTotalM(request.getMaxWalkingTotalM());
        entity.setMinConnectionMinutes(request.getMinConnectionMinutes());
        entity.setMaxTotalCo2Grams(request.getMaxTotalCo2Grams());
        entity.setPreferredModesJson(request.getPreferredModesJson());
        entity.setConstraintsJson(request.getConstraintsJson());

        log.info("Saving constraints for policy set: {}", policySetId);
        return constraintsRepository.save(entity);
    }

    // ── Nodes ─────────────────────────────────────────────────

    @Override
    public List<JourneyPolicyNode> listNodes(UUID policySetId) {
        return nodeRepository.findByPolicySetId(policySetId);
    }

    @Override
    @Transactional
    @CacheEvict(value = PolicyCacheKey.CACHE_NAME, allEntries = true)
    public List<JourneyPolicyNode> saveNodes(UUID policySetId, List<NodeRequest> requests) {
        JourneyPolicySet policySet = getPolicySet(policySetId);
        nodeRepository.deleteAll(nodeRepository.findByPolicySetId(policySetId));
        nodeRepository.flush();

        List<JourneyPolicyNode> entities = requests.stream().map(req -> {
            JourneyPolicyNode node = new JourneyPolicyNode();
            node.setId(UUID.randomUUID());
            node.setPolicySet(policySet);
            node.setNodeKey(EnumNodeKey.valueOf(req.getNodeKey()));
            node.setMinVisits(req.getMinVisits() != null ? req.getMinVisits() : 0);
            node.setMaxVisits(req.getMaxVisits() != null ? req.getMaxVisits() : 1);
            node.setPropsJson(req.getPropsJson());
            node.setUiX(req.getUiX());
            node.setUiY(req.getUiY());
            return node;
        }).collect(Collectors.toList());

        log.info("Saving {} nodes for policy set: {}", entities.size(), policySetId);
        return nodeRepository.saveAll(entities);
    }

    // ── Transitions ───────────────────────────────────────────

    @Override
    public List<JourneyPolicyTransition> listTransitions(UUID policySetId) {
        return transitionRepository.findByPolicySetId(policySetId);
    }

    @Override
    @Transactional
    @CacheEvict(value = PolicyCacheKey.CACHE_NAME, allEntries = true)
    public List<JourneyPolicyTransition> saveTransitions(UUID policySetId, List<TransitionRequest> requests) {
        JourneyPolicySet policySet = getPolicySet(policySetId);
        transitionRepository.deleteAll(transitionRepository.findByPolicySetId(policySetId));
        transitionRepository.flush();

        // Build a node map for lookups
        Map<UUID, JourneyPolicyNode> nodeMap = nodeRepository.findByPolicySetId(policySetId)
                .stream().collect(Collectors.toMap(JourneyPolicyNode::getId, n -> n));

        List<JourneyPolicyTransition> entities = requests.stream().map(req -> {
            JourneyPolicyTransition t = new JourneyPolicyTransition();
            t.setId(UUID.randomUUID());
            t.setPolicySet(policySet);
            JourneyPolicyNode fromNode = nodeMap.get(req.getFromNodeId());
            JourneyPolicyNode toNode = nodeMap.get(req.getToNodeId());
            if (fromNode == null || toNode == null) {
                throw new IllegalArgumentException(
                        "Invalid node reference in transition: from=" + req.getFromNodeId()
                                + ", to=" + req.getToNodeId());
            }
            t.setFromNode(fromNode);
            t.setToNode(toNode);
            t.setPriority(req.getPriority() != null ? req.getPriority() : 1);
            t.setGuardJson(req.getGuardJson());
            t.setUiJson(req.getUiJson());
            return t;
        }).collect(Collectors.toList());

        log.info("Saving {} transitions for policy set: {}", entities.size(), policySetId);
        return transitionRepository.saveAll(entities);
    }
}
