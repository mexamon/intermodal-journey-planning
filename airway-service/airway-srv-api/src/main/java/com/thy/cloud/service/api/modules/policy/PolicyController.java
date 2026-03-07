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

    // ── Policy Set ────────────────────────────────────────────

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

    // ── Constraints ───────────────────────────────────────────

    @GetMapping("/sets/{policySetId}/constraints")
    public Result<JourneyPolicyConstraints> getConstraints(@PathVariable UUID policySetId) {
        return Result.success(policyService.getConstraints(policySetId));
    }

    // ── Nodes (State Machine) ─────────────────────────────────

    @GetMapping("/sets/{policySetId}/nodes")
    public Result<List<JourneyPolicyNode>> listNodes(@PathVariable UUID policySetId) {
        return Result.success(policyService.listNodes(policySetId));
    }

    // ── Transitions (State Machine) ───────────────────────────

    @GetMapping("/sets/{policySetId}/transitions")
    public Result<List<JourneyPolicyTransition>> listTransitions(@PathVariable UUID policySetId) {
        return Result.success(policyService.listTransitions(policySetId));
    }
}
