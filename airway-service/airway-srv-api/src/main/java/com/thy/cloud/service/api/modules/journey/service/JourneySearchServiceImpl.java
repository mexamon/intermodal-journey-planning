package com.thy.cloud.service.api.modules.journey.service;

import com.thy.cloud.service.api.modules.journey.filter.PolicyFilter;
import com.thy.cloud.service.api.modules.journey.graph.GraphBuilder;
import com.thy.cloud.service.api.modules.journey.graph.HubDiscoveryService;
import com.thy.cloud.service.api.modules.journey.model.JourneyResult;
import com.thy.cloud.service.api.modules.journey.model.JourneySearchRequest;
import com.thy.cloud.service.api.modules.journey.result.JourneyRanker;
import com.thy.cloud.service.api.modules.journey.result.ResultMapper;
import com.thy.cloud.service.api.modules.journey.search.BfsPathFinder;
import com.thy.cloud.service.api.modules.policy.service.PolicyResolver;
import com.thy.cloud.service.api.resolver.model.EdgeSearchContext;
import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyConstraints;
import com.thy.cloud.service.dao.entity.transport.TransportMode;
import com.thy.cloud.service.dao.repository.inventory.LocationRepository;
import com.thy.cloud.service.api.modules.transport.service.TransportService;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.thy.cloud.service.api.modules.journey.graph.HubDiscoveryService.locKey;

/**
 * Journey search orchestrator — delegates to focused components:
 * <ul>
 *   <li>{@link HubDiscoveryService} — hub (airport/station) discovery</li>
 *   <li>{@link GraphBuilder} — 3-phase adjacency graph construction</li>
 *   <li>{@link BfsPathFinder} — time-aware BFS path finding</li>
 *   <li>{@link PolicyFilter} — per-hub and route-level filtering</li>
 *   <li>{@link ResultMapper} — path → DTO conversion</li>
 *   <li>{@link JourneyRanker} — sorting, deduplication, labeling</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JourneySearchServiceImpl implements JourneySearchService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final LocationRepository locationRepository;
    private final TransportService transportService;
    private final PolicyResolver policyResolver;

    // Extracted components
    private final HubDiscoveryService hubDiscovery;
    private final GraphBuilder graphBuilder;
    private final BfsPathFinder bfsPathFinder;
    private final PolicyFilter policyFilter;
    private final ResultMapper resultMapper;
    private final JourneyRanker journeyRanker;

    @Override
    public List<JourneyResult> search(JourneySearchRequest request) {
        // 1. Resolve origin & destination
        ResolvedLocation origin = resolveLocation(
                request.getOriginLocationId(), request.getOriginIataCode(),
                request.getOriginQuery(), request.getOriginLat(), request.getOriginLon(),
                request.getOriginType());
        ResolvedLocation destination = resolveLocation(
                request.getDestinationLocationId(), request.getDestinationIataCode(),
                request.getDestinationQuery(), request.getDestinationLat(), request.getDestinationLon(),
                request.getDestinationType());

        log.info("Resolved: {} ({}) → {} ({})",
                origin.name(), origin.source(), destination.name(), destination.source());

        if (origin.id() != null && destination.id() != null && origin.id().equals(destination.id())) {
            return Collections.emptyList();
        }

        LocalDate date = request.getDepartureDate() != null ? request.getDepartureDate() : LocalDate.now();
        LocalTime earliest = request.getEarliestDeparture() != null ? request.getEarliestDeparture() : LocalTime.of(0, 0);

        Set<String> allowedModes = request.getPreferredModes() != null
                ? new HashSet<>(request.getPreferredModes()) : Set.of();

        EdgeSearchContext context = new EdgeSearchContext(date, earliest, 50, allowedModes);

        // 2. Discover hubs
        List<ResolvedLocation> originHubs = hubDiscovery.discoverHubs(origin);
        List<ResolvedLocation> destHubs = hubDiscovery.discoverHubs(destination);

        log.info("Origin hubs: {}, Dest hubs: {}",
                originHubs.stream().map(ResolvedLocation::name).toList(),
                destHubs.stream().map(ResolvedLocation::name).toList());

        // 3. Resolve policies
        PolicyResult policy = resolvePolicy(request, origin, destination, originHubs, destHubs);

        // 4. Build adjacency graph (3-phase)
        Map<String, ResolvedLocation> locationIndex = new HashMap<>();
        Map<String, List<ResolvedEdge>> adjacency = new HashMap<>();

        GraphBuilder.indexLocation(locationIndex, origin);
        GraphBuilder.indexLocation(locationIndex, destination);
        originHubs.forEach(hub -> GraphBuilder.indexLocation(locationIndex, hub));
        destHubs.forEach(hub -> GraphBuilder.indexLocation(locationIndex, hub));

        graphBuilder.buildFirstMile(origin, originHubs, context, adjacency, locationIndex);

        boolean originIsHub = originHubs.stream().anyMatch(h -> locKey(h).equals(locKey(origin)));
        graphBuilder.buildTrunk(originHubs, originIsHub, origin, context, policy.maxTransfers, adjacency, locationIndex);
        graphBuilder.buildLastMile(destHubs, destination, context, adjacency, locationIndex);

        // 5. BFS path finding
        Set<String> destinationKeys = new HashSet<>();
        destinationKeys.add(locKey(destination));
        if ("AIRPORT".equals(destination.type()) || "STATION".equals(destination.type())) {
            destHubs.forEach(hub -> destinationKeys.add(locKey(hub)));
        }

        Map<String, Integer> transferTimes = getTransferTimes();
        List<List<ResolvedEdge>> completePaths = new ArrayList<>();
        bfsPathFinder.findPaths(adjacency, locKey(origin), destinationKeys,
                earliest, policy.maxTransfers, policy.maxDuration, transferTimes, completePaths);

        // Also try starting from each origin hub
        for (ResolvedLocation hub : originHubs) {
            if (locKey(hub).equals(locKey(origin))) continue;
            List<List<ResolvedEdge>> hubPaths = new ArrayList<>();
            bfsPathFinder.findPaths(adjacency, locKey(hub), destinationKeys,
                    earliest, policy.maxTransfers, policy.maxDuration, transferTimes, hubPaths);

            List<ResolvedEdge> firstMile = adjacency.getOrDefault(locKey(origin), List.of())
                    .stream().filter(e -> locKey(e.destination()).equals(locKey(hub))).toList();
            Map<String, Long> fmByMode = firstMile.stream()
                    .collect(Collectors.groupingBy(ResolvedEdge::transportModeCode, Collectors.counting()));
            log.info("  First-mile to {}: {} edges ({}), trunk paths: {}",
                    hub.name(), firstMile.size(), fmByMode, hubPaths.size());

            for (List<ResolvedEdge> trunkPath : hubPaths) {
                for (ResolvedEdge fm : firstMile) {
                    List<ResolvedEdge> fullPath = new ArrayList<>();
                    fullPath.add(fm);
                    fullPath.addAll(trunkPath);
                    completePaths.add(fullPath);
                }
            }
        }

        // 6. Policy filter
        int preFilterCount = completePaths.size();
        completePaths = policyFilter.apply(completePaths, policy.constraints);
        log.info("Policy filter: {} paths → {} paths", preFilterCount, completePaths.size());

        // 7. Convert to results
        List<JourneyResult> results = resultMapper.mapAll(completePaths, date, request.getTargetCurrency());

        // 8. Rank & label
        return journeyRanker.rankAndLabel(results, request.getSortBy());
    }

    // ── Policy Resolution ──

    private record PolicyResult(int maxTransfers, int maxDuration, JourneyPolicyConstraints constraints) {}

    private PolicyResult resolvePolicy(JourneySearchRequest request,
                                        ResolvedLocation origin, ResolvedLocation destination,
                                        List<ResolvedLocation> originHubs, List<ResolvedLocation> destHubs) {
        String originIata = origin.iataCode();
        String destIata = destination.iataCode();

        if (originIata == null) originIata = request.getOriginIataCode();
        if (originIata == null && !originHubs.isEmpty()) {
            originIata = originHubs.stream()
                    .filter(h -> h.iataCode() != null)
                    .map(ResolvedLocation::iataCode)
                    .findFirst().orElse(null);
            log.info("Using origin hub IATA for policy: {}", originIata);
        }
        if (destIata == null) destIata = request.getDestinationIataCode();
        if (destIata == null && !destHubs.isEmpty()) {
            destIata = destHubs.stream()
                    .filter(h -> h.iataCode() != null)
                    .map(ResolvedLocation::iataCode)
                    .findFirst().orElse(null);
            log.info("Using dest hub IATA for policy: {}", destIata);
        }

        JourneyPolicyConstraints constraints = policyResolver.resolveForRoute(originIata, destIata);
        int maxTransfers;
        int maxDuration;
        if (constraints != null) {
            maxTransfers = constraints.getMaxTransfers();
            maxDuration = constraints.getMaxTotalDurationMin() != null
                    ? constraints.getMaxTotalDurationMin()
                    : request.getMaxDurationMinutes();
            log.info("Policy applied: maxLegs={}, maxTransfers={}, maxFlights={}, maxDuration={}min",
                    constraints.getMaxLegs(), maxTransfers, constraints.getMaxFlights(), maxDuration);
        } else {
            maxTransfers = Math.min(request.getMaxTransfers(), 4);
            maxDuration = request.getMaxDurationMinutes();
            log.info("No policy found, using defaults: maxTransfers={}, maxDuration={}min",
                    maxTransfers, maxDuration);
        }

        return new PolicyResult(maxTransfers, maxDuration, constraints);
    }

    // ── Location Resolution ──

    private ResolvedLocation resolveLocation(UUID locationId, String iataCode,
                                              String query, Double lat, Double lon, String type) {
        if (iataCode != null && !iataCode.isBlank()) {
            Location loc = locationRepository.findByIataCode(iataCode.toUpperCase().trim()).orElse(null);
            if (loc != null) return HubDiscoveryService.toResolvedLocation(loc);
        }
        if (locationId != null) {
            Location loc = locationRepository.findById(locationId).orElse(null);
            if (loc != null) return HubDiscoveryService.toResolvedLocation(loc);
        }
        if (lat != null && lon != null && lat != 0 && lon != 0) {
            String name = query != null ? query : "Location";
            String locType = "airport".equals(type) ? "AIRPORT"
                    : "station".equals(type) ? "STATION" : "POI";
            log.info("Location '{}' from frontend: ({}, {}) type={}", name, lat, lon, locType);
            return ResolvedLocation.virtual(name, lat, lon, locType);
        }
        throw new IllegalArgumentException("Location not resolvable. " +
                "Please select a location from the search suggestions. (query=" + query + ")");
    }

    // ── Transfer Times ──

    private static final int DEFAULT_TRANSFER_MIN = 5;

    /**
     * Build transfer times from DB — fully data-driven.
     * Reads "transfer_time_min" from each mode's config_json.
     * Falls back to DEFAULT_TRANSFER_MIN (5 min) if not specified.
     */
    private Map<String, Integer> getTransferTimes() {
        Map<String, Integer> map = new HashMap<>();
        try {
            for (TransportMode tm : transportService.listActiveModes()) {
                int transferMin = DEFAULT_TRANSFER_MIN;
                if (tm.getConfigJson() != null) {
                    JsonNode node = JSON.readTree(tm.getConfigJson());
                    JsonNode ttNode = node.get("transfer_time_min");
                    if (ttNode != null && ttNode.isNumber()) {
                        transferMin = ttNode.intValue();
                    }
                }
                map.put(tm.getCode(), transferMin);
            }
            log.info("Loaded transfer times from DB: {}", map);
        } catch (Exception e) {
            log.warn("Failed to load transfer times from DB: {}", e.getMessage());
        }
        return Collections.unmodifiableMap(map);
    }
}
