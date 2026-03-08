package com.thy.cloud.service.api.modules.policy;

import com.thy.cloud.base.core.api.Result;
import com.thy.cloud.service.api.modules.policy.model.PolicySetSearchRequest;
import com.thy.cloud.service.api.modules.policy.service.PolicyService;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyConstraints;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyNode;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicySet;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyTransition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/policy")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    // ── Policy Set — Read ─────────────────────────────────────

    @PostMapping(value = "/sets/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Page<JourneyPolicySet>> searchPolicySets(@RequestBody @Valid PolicySetSearchRequest request,
            Pageable pageable) {
        return Result.success(policyService.searchPolicySets(request, pageable));
    }

    @GetMapping("/sets/{id}")
    public Result<JourneyPolicySet> getPolicySet(@PathVariable UUID id) {
        return Result.success(policyService.getPolicySet(id));
    }

    @GetMapping("/sets/code/{code}")
    public Result<JourneyPolicySet> getPolicySetByCode(@PathVariable String code) {
        return Result.success(policyService.getPolicySetByCode(code));
    }

    // ── Policy Set — Write ────────────────────────────────────

    @PostMapping(value = "/sets", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<JourneyPolicySet> createPolicySet(@RequestBody @Valid JourneyPolicySet policySet) {
        return Result.success(policyService.createPolicySet(policySet));
    }

    @PutMapping(value = "/sets/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<JourneyPolicySet> updatePolicySet(@PathVariable UUID id,
            @RequestBody @Valid JourneyPolicySet policySet) {
        return Result.success(policyService.updatePolicySet(id, policySet));
    }

    @DeleteMapping("/sets/{id}")
    public Result<Void> deletePolicySet(@PathVariable UUID id) {
        policyService.deletePolicySet(id);
        return Result.success(null);
    }

    // ── Constraints ───────────────────────────────────────────

    @GetMapping("/sets/{policySetId}/constraints")
    public Result<JourneyPolicyConstraints> getConstraints(@PathVariable UUID policySetId) {
        return Result.success(policyService.getConstraints(policySetId));
    }

    @PutMapping(value = "/sets/{policySetId}/constraints", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<JourneyPolicyConstraints> saveConstraints(@PathVariable UUID policySetId,
            @RequestBody @Valid JourneyPolicyConstraints constraints) {
        return Result.success(policyService.saveConstraints(policySetId, constraints));
    }

    // ── Nodes (State Machine) ─────────────────────────────────

    @GetMapping("/sets/{policySetId}/nodes")
    public Result<List<JourneyPolicyNode>> listNodes(@PathVariable UUID policySetId) {
        return Result.success(policyService.listNodes(policySetId));
    }

    @PutMapping(value = "/sets/{policySetId}/nodes", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<JourneyPolicyNode>> saveNodes(@PathVariable UUID policySetId,
            @RequestBody @Valid List<JourneyPolicyNode> nodes) {
        return Result.success(policyService.saveNodes(policySetId, nodes));
    }

    // ── Transitions (State Machine) ───────────────────────────

    @GetMapping("/sets/{policySetId}/transitions")
    public Result<List<JourneyPolicyTransition>> listTransitions(@PathVariable UUID policySetId) {
        return Result.success(policyService.listTransitions(policySetId));
    }

    @PutMapping(value = "/sets/{policySetId}/transitions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<List<JourneyPolicyTransition>> saveTransitions(@PathVariable UUID policySetId,
            @RequestBody @Valid List<JourneyPolicyTransition> transitions) {
        return Result.success(policyService.saveTransitions(policySetId, transitions));
    }
}
