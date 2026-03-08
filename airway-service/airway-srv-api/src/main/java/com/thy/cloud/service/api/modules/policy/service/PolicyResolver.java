package com.thy.cloud.service.api.modules.policy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyConstraints;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicySet;
import com.thy.cloud.service.dao.enums.EnumPolicyScopeType;
import com.thy.cloud.service.dao.enums.EnumPolicyStatus;
import com.thy.cloud.service.dao.repository.policy.JourneyPolicyConstraintsRepository;
import com.thy.cloud.service.dao.repository.policy.JourneyPolicySetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Resolves the effective journey policy for a given route or airport hub.
 * <p>
 * Resolution priority (most specific → least specific):
 *   AIRPORT_PAIR > AIRPORT > COUNTRY > GLOBAL
 * <p>
 * Only ACTIVE policies are considered. If no policy matches,
 * null is returned and the caller should use defaults.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PolicyResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DEFAULT_MAX_MILE_EDGES = 2;

    private final JourneyPolicySetRepository policySetRepository;
    private final JourneyPolicyConstraintsRepository constraintsRepository;

    /**
     * Resolve the constraints for a route between two airports.
     * Tries: AIRPORT_PAIR(origin-dest) → AIRPORT(origin) → AIRPORT(dest) → GLOBAL(*)
     */
    public JourneyPolicyConstraints resolveForRoute(String originIata, String destIata) {
        if (originIata == null && destIata == null) {
            return resolveGlobal();
        }

        // 1. Try AIRPORT_PAIR (e.g. SAW-LHR)
        if (originIata != null && destIata != null) {
            String pairKey = originIata + "-" + destIata;
            JourneyPolicyConstraints result = lookupConstraints(EnumPolicyScopeType.AIRPORT_PAIR, pairKey);
            if (result != null) {
                log.debug("PolicyResolver: matched AIRPORT_PAIR={}", pairKey);
                return result;
            }
        }

        // 2. Try AIRPORT (origin side)
        if (originIata != null) {
            JourneyPolicyConstraints result = lookupConstraints(EnumPolicyScopeType.AIRPORT, originIata);
            if (result != null) {
                log.debug("PolicyResolver: matched AIRPORT={} (origin)", originIata);
                return result;
            }
        }

        // 3. Try AIRPORT (destination side)
        if (destIata != null) {
            JourneyPolicyConstraints result = lookupConstraints(EnumPolicyScopeType.AIRPORT, destIata);
            if (result != null) {
                log.debug("PolicyResolver: matched AIRPORT={} (dest)", destIata);
                return result;
            }
        }

        // 4. Fallback to GLOBAL
        return resolveGlobal();
    }

    /**
     * Resolve constraints for a single airport hub.
     * Used for per-hub first-mile / last-mile enforcement.
     */
    public JourneyPolicyConstraints resolveForAirport(String iataCode) {
        if (iataCode == null) return resolveGlobal();

        JourneyPolicyConstraints result = lookupConstraints(EnumPolicyScopeType.AIRPORT, iataCode);
        if (result != null) return result;

        return resolveGlobal();
    }

    /**
     * Get max allowed first-mile edges for a hub airport.
     * Reads from constraints_json.max_first_mile_edges, falls back to DEFAULT_MAX_MILE_EDGES.
     */
    public int getMaxFirstMileEdges(String hubIata) {
        JourneyPolicyConstraints constraints = resolveForAirport(hubIata);
        if (constraints == null) return DEFAULT_MAX_MILE_EDGES;
        return readJsonInt(constraints.getConstraintsJson(), "max_first_mile_edges", DEFAULT_MAX_MILE_EDGES);
    }

    /**
     * Get max allowed last-mile edges for a hub airport.
     * Reads from constraints_json.max_last_mile_edges, falls back to DEFAULT_MAX_MILE_EDGES.
     */
    public int getMaxLastMileEdges(String hubIata) {
        JourneyPolicyConstraints constraints = resolveForAirport(hubIata);
        if (constraints == null) return DEFAULT_MAX_MILE_EDGES;
        return readJsonInt(constraints.getConstraintsJson(), "max_last_mile_edges", DEFAULT_MAX_MILE_EDGES);
    }

    /**
     * Resolve the global fallback policy.
     */
    public JourneyPolicyConstraints resolveGlobal() {
        JourneyPolicyConstraints result = lookupConstraints(EnumPolicyScopeType.GLOBAL, "*");
        if (result != null) {
            log.debug("PolicyResolver: using GLOBAL fallback");
        }
        return result;
    }

    // ─── Internal ──────────────────────────────────────────

    private JourneyPolicyConstraints lookupConstraints(EnumPolicyScopeType scopeType, String scopeKey) {
        List<JourneyPolicySet> sets = policySetRepository.findByScopeTypeAndScopeKeyAndStatus(
                scopeType, scopeKey, EnumPolicyStatus.ACTIVE);

        if (sets.isEmpty()) return null;

        JourneyPolicySet policySet = sets.get(0);
        Optional<JourneyPolicyConstraints> constraints = constraintsRepository.findByPolicySetId(policySet.getId());
        return constraints.orElse(null);
    }

    /**
     * Parse an integer value from a JSONB string.
     */
    private int readJsonInt(String json, String field, int defaultValue) {
        if (json == null || json.isBlank()) return defaultValue;
        try {
            JsonNode node = MAPPER.readTree(json);
            JsonNode value = node.get(field);
            if (value != null && value.isInt()) return value.intValue();
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse constraints_json: {}", e.getMessage());
        }
        return defaultValue;
    }
}
