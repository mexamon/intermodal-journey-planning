package com.thy.cloud.service.api.modules.journey.filter;

import com.thy.cloud.service.api.modules.policy.service.PolicyResolver;
import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyConstraints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Filters BFS paths by per-hub and route-level policy constraints.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyFilter {

    private final PolicyResolver policyResolver;

    /**
     * Filter paths by policy constraints:
     * - Route-level: max_legs, min_flights, max_flights
     * - Per origin hub: max first-mile edges (before first FLIGHT)
     * - Per dest hub: max last-mile edges (after last FLIGHT)
     */
    public List<List<ResolvedEdge>> apply(List<List<ResolvedEdge>> paths,
                                           JourneyPolicyConstraints routeConstraints) {
        List<List<ResolvedEdge>> filtered = new ArrayList<>();

        Map<String, Integer> firstMileCache = new HashMap<>();
        Map<String, Integer> lastMileCache = new HashMap<>();

        for (List<ResolvedEdge> path : paths) {
            if (path.isEmpty()) continue;

            // Classify edges: find first/last FLIGHT indices
            int firstFlightIdx = -1;
            int lastFlightIdx = -1;
            int flightCount = 0;

            for (int i = 0; i < path.size(); i++) {
                if ("FLIGHT".equals(path.get(i).transportModeCode())) {
                    if (firstFlightIdx == -1) firstFlightIdx = i;
                    lastFlightIdx = i;
                    flightCount++;
                }
            }

            // Must have at least one flight
            if (firstFlightIdx == -1) {
                log.debug("Policy filter: path rejected — no FLIGHT edge");
                continue;
            }

            int firstMileEdges = firstFlightIdx;
            int lastMileEdges = path.size() - lastFlightIdx - 1;
            int totalLegs = path.size();

            // ── Route-level constraints ──
            if (routeConstraints != null) {
                if (totalLegs > routeConstraints.getMaxLegs()) {
                    log.debug("Policy filter: path rejected — {} legs > max_legs {}",
                            totalLegs, routeConstraints.getMaxLegs());
                    continue;
                }
                if (flightCount < routeConstraints.getMinFlights()) {
                    log.debug("Policy filter: path rejected — {} flights < min_flights {}",
                            flightCount, routeConstraints.getMinFlights());
                    continue;
                }
                if (flightCount > routeConstraints.getMaxFlights()) {
                    log.debug("Policy filter: path rejected — {} flights > max_flights {}",
                            flightCount, routeConstraints.getMaxFlights());
                    continue;
                }
            }

            // ── Per-hub: first-mile constraint ──
            ResolvedEdge firstFlight = path.get(firstFlightIdx);
            String originHubIata = firstFlight.origin().iataCode();

            if (originHubIata != null && firstMileEdges > 0) {
                int maxFirstMile = firstMileCache.computeIfAbsent(originHubIata,
                        iata -> policyResolver.getMaxFirstMileEdges(iata));
                if (firstMileEdges > maxFirstMile) {
                    log.debug("Policy filter: path rejected — {} first-mile edges > {} hub {} max",
                            firstMileEdges, maxFirstMile, originHubIata);
                    continue;
                }
            }

            // ── Per-hub: last-mile constraint ──
            ResolvedEdge lastFlight = path.get(lastFlightIdx);
            String destHubIata = lastFlight.destination().iataCode();

            if (destHubIata != null && lastMileEdges > 0) {
                int maxLastMile = lastMileCache.computeIfAbsent(destHubIata,
                        iata -> policyResolver.getMaxLastMileEdges(iata));
                if (lastMileEdges > maxLastMile) {
                    log.debug("Policy filter: path rejected — {} last-mile edges > {} hub {} max",
                            lastMileEdges, maxLastMile, destHubIata);
                    continue;
                }
            }

            filtered.add(path);
        }

        return filtered;
    }
}
