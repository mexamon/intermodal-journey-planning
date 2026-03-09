package com.thy.cloud.service.api.modules.journey.service;

import com.thy.cloud.service.api.modules.journey.model.JourneyResult;
import com.thy.cloud.service.api.modules.journey.model.JourneySearchRequest;
import com.thy.cloud.service.api.modules.journey.model.JourneySegment;
import com.thy.cloud.service.api.resolver.edge.EdgeResolver;
import com.thy.cloud.service.api.resolver.location.LocationResolver;
import com.thy.cloud.service.api.resolver.model.EdgeSearchContext;
import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import com.thy.cloud.service.api.util.CurrencyConverter;
import com.thy.cloud.service.api.modules.policy.service.PolicyResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thy.cloud.service.dao.entity.policy.JourneyPolicyConstraints;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.entity.transport.EdgeTrip;
import com.thy.cloud.service.dao.entity.transport.TransportationEdge;
import com.thy.cloud.service.dao.entity.transport.TransportMode;
import com.thy.cloud.service.dao.enums.EnumEdgeStatus;
import com.thy.cloud.service.dao.enums.EnumLocationType;
import com.thy.cloud.service.dao.repository.inventory.LocationRepository;
import com.thy.cloud.service.dao.repository.transport.TransportModeRepository;
import com.thy.cloud.service.dao.repository.transport.TransportationEdgeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JourneySearchServiceImpl implements JourneySearchService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final TransportationEdgeRepository edgeRepository;
    private final LocationRepository locationRepository;
    private final TransportModeRepository transportModeRepository;

    // Resolver chains (injected by Spring — ordered by @Order or registration order)
    private final List<LocationResolver> locationResolvers;
    private final List<EdgeResolver> edgeResolvers;
    private final PolicyResolver policyResolver;

    // Transfer times — loaded from DB on first access (transport_mode.config_json.transfer_time_min)
    private volatile Map<String, Integer> transferTimesCache;

    private Map<String, Integer> getTransferTimes() {
        if (transferTimesCache != null) return transferTimesCache;
        synchronized (this) {
            if (transferTimesCache != null) return transferTimesCache;
            Map<String, Integer> map = new HashMap<>();
            // Hardcoded fallbacks in case DB has no config_json
            map.put("FLIGHT", 60); map.put("TRAIN", 15); map.put("BUS", 10);
            map.put("SUBWAY", 5);  map.put("FERRY", 15); map.put("WALKING", 0);
            map.put("UBER", 5);    map.put("TAXI", 5);   map.put("BIKE", 5);
            try {
                for (TransportMode tm : transportModeRepository.findByIsActiveTrueOrderBySortOrderAsc()) {
                    if (tm.getConfigJson() != null) {
                        JsonNode node = JSON.readTree(tm.getConfigJson());
                        JsonNode ttNode = node.get("transfer_time_min");
                        if (ttNode != null && ttNode.isNumber()) {
                            map.put(tm.getCode(), ttNode.intValue());
                        }
                    }
                }
                log.info("Loaded transfer times from DB: {}", map);
            } catch (Exception e) {
                log.warn("Failed to load transfer times from DB, using defaults: {}", e.getMessage());
            }
            transferTimesCache = Collections.unmodifiableMap(map);
            return transferTimesCache;
        }
    }

    private static final int MAX_RESULTS = 10;
    private static final double HUB_SEARCH_RADIUS_M = 50_000; // 50km for hub discovery

    @Override
    public List<JourneyResult> search(JourneySearchRequest request) {
        // 1. Resolve origin & destination — frontend tells us the type
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

        // 2. Discover hubs (nearby airports/stations)
        List<ResolvedLocation> originHubs = discoverHubs(origin);
        List<ResolvedLocation> destHubs = discoverHubs(destination);

        log.info("Origin hubs: {}, Dest hubs: {}",
                originHubs.stream().map(ResolvedLocation::name).toList(),
                destHubs.stream().map(ResolvedLocation::name).toList());

        // ── Policy Resolution (after hub discovery so we can use hub IATA codes) ──
        String originIata = origin.iataCode();
        String destIata = destination.iataCode();
        // For non-airport origins/destinations, use nearest hub IATA for policy lookup
        if (originIata == null) {
            originIata = request.getOriginIataCode();
        }
        if (originIata == null && !originHubs.isEmpty()) {
            originIata = originHubs.stream()
                    .filter(h -> h.iataCode() != null)
                    .map(ResolvedLocation::iataCode)
                    .findFirst().orElse(null);
            log.info("Using origin hub IATA for policy: {}", originIata);
        }
        if (destIata == null) {
            destIata = request.getDestinationIataCode();
        }
        if (destIata == null && !destHubs.isEmpty()) {
            destIata = destHubs.stream()
                    .filter(h -> h.iataCode() != null)
                    .map(ResolvedLocation::iataCode)
                    .findFirst().orElse(null);
            log.info("Using dest hub IATA for policy: {}", destIata);
        }

        JourneyPolicyConstraints policyConstraints = policyResolver.resolveForRoute(originIata, destIata);
        int maxTransfers;
        int maxDuration;
        if (policyConstraints != null) {
            maxTransfers = policyConstraints.getMaxTransfers();
            maxDuration = policyConstraints.getMaxTotalDurationMin() != null
                    ? policyConstraints.getMaxTotalDurationMin()
                    : request.getMaxDurationMinutes();
            log.info("Policy applied: maxLegs={}, maxTransfers={}, maxFlights={}, maxDuration={}min",
                    policyConstraints.getMaxLegs(), maxTransfers,
                    policyConstraints.getMaxFlights(), maxDuration);
        } else {
            maxTransfers = Math.min(request.getMaxTransfers(), 4);
            maxDuration = request.getMaxDurationMinutes();
            log.info("No policy found, using defaults: maxTransfers={}, maxDuration={}min",
                    maxTransfers, maxDuration);
        }

        // 3. Build adjacency map with 3-phase approach:
        //    Phase A: First-mile (origin → origin hubs via computed/GTFS)
        //    Phase B: Trunk     (hub → hub via Static/Amadeus BFS)
        //    Phase C: Last-mile (dest hubs → destination via computed)
        Map<String, ResolvedLocation> locationIndex = new HashMap<>();
        Map<String, List<ResolvedEdge>> adjacency = new HashMap<>();

        indexLocation(locationIndex, origin);
        indexLocation(locationIndex, destination);
        originHubs.forEach(hub -> indexLocation(locationIndex, hub));
        destHubs.forEach(hub -> indexLocation(locationIndex, hub));

        // ── Phase A: First-mile edges (origin → each origin hub) ──
        log.info("Phase A: {} origin hubs for first-mile from '{}'", originHubs.size(), origin.name());
        for (ResolvedLocation hub : originHubs) {
            log.info("  Hub: {} (locKey={}, originLocKey={})", hub.name(), locKey(hub), locKey(origin));
            if (locKey(hub).equals(locKey(origin))) {
                log.info("  → Skipping (origin IS this hub)");
                continue;
            }
            for (EdgeResolver resolver : edgeResolvers) {
                String res = resolver.getResolution();
                if ("COMPUTED".equals(res) || "GTFS_LIVE".equals(res)) {
                    try {
                        List<ResolvedEdge> edges = resolver.resolve(origin, hub, context);
                        // Remap GTFS edges whose destination is near the hub (< 2km)
                        // so BFS can naturally connect GTFS stops to airport hubs
                        if ("GTFS_LIVE".equals(res)) {
                            List<ResolvedEdge> remapped = new ArrayList<>();
                            for (ResolvedEdge edge : edges) {
                                double dist = haversineM(
                                        edge.destination().lat(), edge.destination().lon(),
                                        hub.lat(), hub.lon());
                                if (dist < 2000) {
                                    // Replace destination with the actual hub
                                    remapped.add(new ResolvedEdge(
                                            edge.id(), edge.origin(), hub,
                                            edge.transportModeCode(), edge.providerCode(),
                                            edge.serviceCode(), edge.departureTime(),
                                            edge.arrivalTime(), edge.durationMin(),
                                            edge.distanceM(), edge.costCents(),
                                            edge.currency(), edge.co2Grams(),
                                            edge.source(), edge.persisted(), edge.attrs()));
                                } else {
                                    remapped.add(edge);
                                }
                            }
                            edges = remapped;
                        }
                        log.info("  → {} first-mile edges from {} via {}", edges.size(), hub.name(), res);
                        addEdges(adjacency, locKey(origin), edges, locationIndex);
                    } catch (Exception e) {
                        log.warn("First-mile {} failed: {}", res, e.getMessage());
                    }
                }
            }
        }

        // If origin is already a hub, add it directly to BFS start
        boolean originIsHub = originHubs.stream().anyMatch(h -> locKey(h).equals(locKey(origin)));

        // ── Phase B: Trunk — BFS using STATIC + AMADEUS resolvers only ──
        log.info("Phase B: Trunk BFS starting from {} hubs, originIsHub={}", originHubs.size(), originIsHub);
        {
            Set<String> visited = new HashSet<>();
            Set<String> frontier = new HashSet<>();

            // Start from all origin hubs (these are airports/stations)
            if (originIsHub) {
                frontier.add(locKey(origin));
            }
            for (ResolvedLocation hub : originHubs) {
                frontier.add(locKey(hub));
            }

            // Build IATA→ResolvedLocation map for matching Amadeus virtual destinations to persisted hubs
            Map<String, ResolvedLocation> iataIndex = new HashMap<>();
            for (ResolvedLocation rl : locationIndex.values()) {
                if (rl.iataCode() != null && !rl.iataCode().isBlank()) {
                    iataIndex.putIfAbsent(rl.iataCode(), rl);
                }
            }

            for (int hop = 0; hop <= maxTransfers; hop++) {
                if (frontier.isEmpty()) break;
                Set<String> nextFrontier = new HashSet<>();

                for (String locKeyStr : frontier) {
                    if (visited.contains(locKeyStr)) continue;
                    visited.add(locKeyStr);

                    ResolvedLocation loc = locationIndex.get(locKeyStr);
                    if (loc == null) continue;

                    // Only STATIC and AMADEUS for trunk routes
                    for (EdgeResolver resolver : edgeResolvers) {
                        String res = resolver.getResolution();
                        if (!"STATIC".equals(res) && !"AMADEUS".equals(res) && !"AMADEUS_LIVE".equals(res)) continue;

                        try {
                            List<ResolvedEdge> edges = resolver.resolve(loc, null, context);

                            // Resolve virtual destinations to persisted locations via IATA code
                            List<ResolvedEdge> resolvedEdges = new ArrayList<>();
                            for (ResolvedEdge edge : edges) {
                                ResolvedLocation dest = edge.destination();
                                if (!dest.persisted() && dest.iataCode() != null && iataIndex.containsKey(dest.iataCode())) {
                                    // Replace virtual destination with persisted one
                                    dest = iataIndex.get(dest.iataCode());
                                    edge = new ResolvedEdge(
                                            edge.id(), edge.origin(), dest,
                                            edge.transportModeCode(), edge.providerCode(), edge.serviceCode(),
                                            edge.departureTime(), edge.arrivalTime(),
                                            edge.durationMin(), edge.distanceM(),
                                            edge.costCents(), edge.currency(), edge.co2Grams(),
                                            edge.source(), edge.persisted(), edge.attrs()
                                    );
                                } else if (!dest.persisted() && dest.name() != null) {
                                    // Try extracting IATA from name like "LHR Airport"
                                    String possibleIata = dest.name().replace(" Airport", "").trim();
                                    if (possibleIata.length() == 3 && iataIndex.containsKey(possibleIata)) {
                                        dest = iataIndex.get(possibleIata);
                                        edge = new ResolvedEdge(
                                                edge.id(), edge.origin(), dest,
                                                edge.transportModeCode(), edge.providerCode(), edge.serviceCode(),
                                                edge.departureTime(), edge.arrivalTime(),
                                                edge.durationMin(), edge.distanceM(),
                                                edge.costCents(), edge.currency(), edge.co2Grams(),
                                                edge.source(), edge.persisted(), edge.attrs()
                                        );
                                    }
                                }
                                resolvedEdges.add(edge);
                            }

                            addEdges(adjacency, locKeyStr, resolvedEdges, locationIndex);

                            for (ResolvedEdge edge : resolvedEdges) {
                                String dKey = locKey(edge.destination());
                                indexLocation(locationIndex, edge.destination());
                                // Also add new persisted locations to IATA index
                                if (edge.destination().iataCode() != null) {
                                    iataIndex.putIfAbsent(edge.destination().iataCode(), edge.destination());
                                }
                                if (!visited.contains(dKey)) {
                                    nextFrontier.add(dKey);
                                }
                            }
                        } catch (Exception e) {
                            log.warn("Trunk {} failed for {}: {}", res, loc.name(), e.getMessage());
                        }
                    }
                }
                frontier = nextFrontier;
            }
        }

        // ── Phase C: Last-mile edges (each dest hub → destination) ──
        // Also consider STATIC edges (TRAIN, BUS, SUBWAY) from airport hubs to nearby stations
        log.info("Phase C: {} destination hubs to process", destHubs.size());
        for (ResolvedLocation hub : destHubs) {
            if (locKey(hub).equals(locKey(destination))) continue;
            for (EdgeResolver resolver : edgeResolvers) {
                String res = resolver.getResolution();
                if ("COMPUTED".equals(res) || "GTFS_LIVE".equals(res) || "STATIC".equals(res)) {
                    try {
                        // STATIC: pass null destination to get ALL outbound edges (to intermediate stations)
                        // COMPUTED/GTFS: pass actual destination for point-to-point edges
                        ResolvedLocation resolverDest = "STATIC".equals(res) ? null : destination;
                        List<ResolvedEdge> edges = resolver.resolve(hub, resolverDest, context);

                        // For STATIC: keep only ground transport (TRAIN, SUBWAY, BUS, FERRY),
                        // exclude FLIGHT (already handled in Phase B trunk)
                        if ("STATIC".equals(res)) {
                            edges = edges.stream()
                                    .filter(e -> !"FLIGHT".equals(e.transportModeCode()))
                                    .toList();
                        }

                        log.info("  Phase C [{}] from {}: {} edges", res, hub.name(), edges.size());
                        addEdges(adjacency, locKey(hub), edges, locationIndex);
                        for (ResolvedEdge edge : edges) {
                            indexLocation(locationIndex, edge.destination());
                        }
                    } catch (Exception e) {
                        log.warn("Last-mile {} failed: {}", res, e.getMessage());
                    }
                }
            }
        }

        // ── Phase C.2: Extend last-mile from intermediate STATIONS to destination ──
        // If STATIC edges lead to stations (e.g. BER→Berlin Hbf via TRAIN),
        // add COMPUTED edges from those stations to the final destination
        Set<String> intermediateKeys = new HashSet<>();
        for (ResolvedLocation hub : destHubs) {
            List<ResolvedEdge> hubEdges = adjacency.getOrDefault(locKey(hub), List.of());
            for (ResolvedEdge edge : hubEdges) {
                String destEdgeKey = locKey(edge.destination());
                // Only extend from STATION-type intermediates (not airports — those are trunk nodes)
                ResolvedLocation intermediate = locationIndex.get(destEdgeKey);
                if (intermediate == null) continue;
                if ("AIRPORT".equals(intermediate.type())) continue;
                if (destEdgeKey.equals(locKey(destination))) continue;
                if (intermediateKeys.contains(destEdgeKey)) continue;

                intermediateKeys.add(destEdgeKey);
                log.info("  Phase C.2: extending from {} to destination", intermediate.name());
                for (EdgeResolver resolver : edgeResolvers) {
                    if ("COMPUTED".equals(resolver.getResolution())) {
                        try {
                            List<ResolvedEdge> computedEdges = resolver.resolve(intermediate, destination, context);
                            log.info("    → {} COMPUTED edges from {}", computedEdges.size(), intermediate.name());
                            addEdges(adjacency, destEdgeKey, computedEdges, locationIndex);
                        } catch (Exception e) {
                            log.warn("Last-mile extension from {} failed: {}", intermediate.name(), e.getMessage());
                        }
                    }
                }
            }
        }

        // 4. BFS path finding — from origin (or hubs) to destination (or dest hubs)
        Set<String> startKeys = new HashSet<>();
        startKeys.add(locKey(origin));

        Set<String> destinationKeys = new HashSet<>();
        destinationKeys.add(locKey(destination));
        // Only treat dest hubs as terminals when destination IS a hub (airport/station)
        // When destination is a place (e.g. "London"), BFS must continue through last-mile edges
        if ("AIRPORT".equals(destination.type()) || "STATION".equals(destination.type())) {
            destHubs.forEach(hub -> destinationKeys.add(locKey(hub)));
        }

        List<List<ResolvedEdge>> completePaths = new ArrayList<>();
        bfs(adjacency, locKey(origin), destinationKeys, date, earliest,
                maxTransfers, maxDuration, completePaths);

        // Also try starting from each origin hub (for direct trunk routes)
        for (ResolvedLocation hub : originHubs) {
            if (locKey(hub).equals(locKey(origin))) continue;
            // Add first-mile + trunk paths
            List<List<ResolvedEdge>> hubPaths = new ArrayList<>();
            bfs(adjacency, locKey(hub), destinationKeys, date, earliest,
                    maxTransfers, maxDuration, hubPaths);
            // Prepend first-mile edges
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

        // 5. Apply per-hub policy constraints (filter invalid paths)
        int preFilterCount = completePaths.size();
        completePaths = applyHubPolicies(completePaths, policyConstraints);
        log.info("Policy filter: {} paths → {} paths", preFilterCount, completePaths.size());

        // 6. Convert paths to JourneyResults
        List<JourneyResult> results = completePaths.stream()
                .map(path -> buildResult(path, date, request.getTargetCurrency()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 7. Sort & label
        return rankAndLabel(results, request.getSortBy());
    }

    /* ═══════════════════════════════════════════════
       Location Resolution — Frontend tells us the type
       ═══════════════════════════════════════════════ */
    private ResolvedLocation resolveLocation(UUID locationId, String iataCode,
                                              String query, Double lat, Double lon, String type) {
        // 1. Airport/Station with IATA → direct DB lookup
        if (iataCode != null && !iataCode.isBlank()) {
            Location loc = locationRepository.findByIataCode(iataCode.toUpperCase().trim()).orElse(null);
            if (loc != null) return toResolvedLocation(loc);
        }

        // 2. DB location by UUID
        if (locationId != null) {
            Location loc = locationRepository.findById(locationId).orElse(null);
            if (loc != null) return toResolvedLocation(loc);
        }

        // 3. Place/POI with coordinates → virtual location (from Google Places autocomplete)
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

    /* ═══════════════════════════════════════════════
       Hub Discovery — Find nearest airports/stations
       ═══════════════════════════════════════════════ */
    private List<ResolvedLocation> discoverHubs(ResolvedLocation location) {
        List<ResolvedLocation> hubs = new ArrayList<>();

        // If it's already an airport/station, it IS a hub
        if ("AIRPORT".equals(location.type()) || "STATION".equals(location.type())) {
            hubs.add(location);
            // Also find other nearby airports if this is a city with multiple airports
            if (location.persisted()) {
                findNearbyPersistedLocations(location, "AIRPORT", 7).forEach(hub -> {
                    if (!locKey(hub).equals(locKey(location))) hubs.add(hub);
                });
            }
            return hubs;
        }

        // For POI/CITY — find nearby airports and stations
        hubs.addAll(findNearbyPersistedLocations(location, "AIRPORT", 7));
        hubs.addAll(findNearbyPersistedLocations(location, "STATION", 5));

        if (hubs.isEmpty()) {
            log.warn("No hubs found near {} ({}, {})", location.name(), location.lat(), location.lon());
        }

        return hubs;
    }

    private List<ResolvedLocation> findNearbyPersistedLocations(ResolvedLocation origin, String type, int limit) {
        // Bounding box ~100km around origin (1 degree ≈ 111km)
        double radiusDeg = (double) HUB_SEARCH_RADIUS_M / 111_000.0;
        double minLat = origin.lat() - radiusDeg;
        double maxLat = origin.lat() + radiusDeg;
        double minLon = origin.lon() - radiusDeg;
        double maxLon = origin.lon() + radiusDeg;

        EnumLocationType locType = EnumLocationType.valueOf(type);
        List<Location> candidates = locationRepository.findNearbyByType(locType, minLat, maxLat, minLon, maxLon);

        log.info("Hub search: {} {} locations in bounding box ({},{})→({},{})",
                candidates.size(), type, String.format("%.2f", minLat), String.format("%.2f", minLon),
                String.format("%.2f", maxLat), String.format("%.2f", maxLon));

        return candidates.stream()
                .filter(loc -> loc.getLat() != null && loc.getLon() != null)
                .map(loc -> {
                    double dist = haversineM(
                            origin.lat(), origin.lon(),
                            loc.getLat().doubleValue(), loc.getLon().doubleValue());
                    log.info("  {} ({}): {}m — {}", loc.getName(),
                            loc.getIataCode(), (int) dist, dist <= HUB_SEARCH_RADIUS_M ? "IN RANGE" : "TOO FAR");
                    return Map.entry(toResolvedLocation(loc), dist);
                })
                .filter(e -> e.getValue() <= HUB_SEARCH_RADIUS_M)
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    /* ═══════════════════════════════════════════════
       BFS Traversal with Time Awareness
       ═══════════════════════════════════════════════ */
    private void bfs(
            Map<String, List<ResolvedEdge>> adjacency,
            String originKey, Set<String> destinationKeys,
            LocalDate date, LocalTime earliestDeparture,
            int maxTransfers, int maxDurationMinutes,
            List<List<ResolvedEdge>> completePaths
    ) {
        record BfsState(String locationKey, List<ResolvedEdge> path,
                        LocalTime earliestDep, int totalMinutes) {}

        final int MAX_BFS_ITERATIONS = 3000;
        final int MAX_QUEUE_SIZE = 1000;
        final int ENOUGH_PATHS = 50;

        Queue<BfsState> queue = new LinkedList<>();
        queue.add(new BfsState(originKey, new ArrayList<>(), earliestDeparture, 0));

        // Track best depth to reach each location — prune worse attempts
        Map<String, Integer> bestDepth = new HashMap<>();
        bestDepth.put(originKey, 0);

        int iterations = 0;
        while (!queue.isEmpty()) {
            if (++iterations > MAX_BFS_ITERATIONS) {
                log.warn("BFS iteration limit reached ({}) — returning {} paths", MAX_BFS_ITERATIONS, completePaths.size());
                break;
            }
            // Soft cap: once we have enough paths, stop expanding
            if (completePaths.size() >= ENOUGH_PATHS) {
                log.info("BFS soft cap: {} paths found, stopping", completePaths.size());
                break;
            }

            BfsState state = queue.poll();

            if (state.path.size() > maxTransfers + 1) continue;
            if (state.totalMinutes > maxDurationMinutes) continue;

            List<ResolvedEdge> outgoing = adjacency.getOrDefault(state.locationKey, Collections.emptyList());

            for (ResolvedEdge edge : outgoing) {
                String destKey = locKey(edge.destination());

                // Avoid cycles
                if (isLocationInPath(state.path, destKey)) continue;

                // Visited-depth pruning: allow +1 depth tolerance for reaching same node
                // This ensures 4-segment station paths (BER→Hbf→dest) aren't pruned by
                // 3-segment direct paths (BER→dest) that reach dest at lower depth
                int newDepth = state.path.size() + 1;
                Integer prev = bestDepth.get(destKey);
                if (prev != null && prev + 1 < newDepth && !destinationKeys.contains(destKey)) continue;
                if (prev == null || newDepth < prev) bestDepth.put(destKey, newDepth);

                // Time compatibility
                LocalTime depTime = edge.departureTime();
                LocalTime arrTime = edge.arrivalTime();

                if (depTime != null && arrTime != null) {
                    // Fixed-schedule: check departure >= earliest
                    if (depTime.isBefore(state.earliestDep)) continue;

                    int dur = (int) depTime.until(arrTime, ChronoUnit.MINUTES);
                    if (dur <= 0) dur += 24 * 60;
                    int newTotal = state.totalMinutes + dur;
                    if (newTotal > maxDurationMinutes) continue;

                    List<ResolvedEdge> newPath = new ArrayList<>(state.path);
                    newPath.add(edge);

                    if (destinationKeys.contains(destKey)) {
                        completePaths.add(newPath);
                    } else if (newPath.size() <= maxTransfers && queue.size() < MAX_QUEUE_SIZE) {
                        int transferMin = getTransferTimes().getOrDefault(edge.transportModeCode(), 10);
                        queue.add(new BfsState(destKey, newPath,
                                arrTime.plusMinutes(transferMin), newTotal + transferMin));
                    }
                } else {
                    // On-demand/computed edge: depart at earliest
                    int dur = edge.durationMin() > 0 ? edge.durationMin() : 15;
                    LocalTime dep = state.earliestDep;
                    LocalTime arr = dep.plusMinutes(dur);
                    int newTotal = state.totalMinutes + dur;
                    if (newTotal > maxDurationMinutes) continue;

                    // Create edge copy with populated times
                    ResolvedEdge timedEdge = new ResolvedEdge(
                            edge.id(), edge.origin(), edge.destination(),
                            edge.transportModeCode(), edge.providerCode(), edge.serviceCode(),
                            dep, arr, dur, edge.distanceM(),
                            edge.costCents(), edge.currency(), edge.co2Grams(),
                            edge.source(), edge.persisted(), edge.attrs()
                    );

                    List<ResolvedEdge> newPath = new ArrayList<>(state.path);
                    newPath.add(timedEdge);

                    if (destinationKeys.contains(destKey)) {
                        completePaths.add(newPath);
                    } else if (newPath.size() <= maxTransfers && queue.size() < MAX_QUEUE_SIZE) {
                        int transferMin = getTransferTimes().getOrDefault(edge.transportModeCode(), 10);
                        queue.add(new BfsState(destKey, newPath,
                                arr.plusMinutes(transferMin), newTotal + transferMin));
                    }
                }
            }
        }
    }

    /* ═══════════════════════════════════════════════
       Per-Hub Policy Filter — applied after BFS
       ═══════════════════════════════════════════════ */

    /**
     * Filter BFS paths by per-hub policy constraints:
     * - Per origin hub: max first-mile edges (before first FLIGHT)
     * - Per dest hub: max last-mile edges (after last FLIGHT)
     * - Route-level: max_legs, min_flights, max_flights
     */
    private List<List<ResolvedEdge>> applyHubPolicies(List<List<ResolvedEdge>> paths,
                                                       JourneyPolicyConstraints routeConstraints) {
        List<List<ResolvedEdge>> filtered = new ArrayList<>();

        // Cache per-hub policy lookups to avoid repeated DB queries (N+1 fix)
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

            int firstMileEdges = firstFlightIdx;           // edges before first FLIGHT
            int lastMileEdges = path.size() - lastFlightIdx - 1; // edges after last FLIGHT
            int totalLegs = path.size();

            // ── Route-level constraints ──
            if (routeConstraints != null) {
                // max_legs
                if (totalLegs > routeConstraints.getMaxLegs()) {
                    log.debug("Policy filter: path rejected — {} legs > max_legs {}",
                            totalLegs, routeConstraints.getMaxLegs());
                    continue;
                }

                // min_flights
                if (flightCount < routeConstraints.getMinFlights()) {
                    log.debug("Policy filter: path rejected — {} flights < min_flights {}",
                            flightCount, routeConstraints.getMinFlights());
                    continue;
                }

                // max_flights
                if (flightCount > routeConstraints.getMaxFlights()) {
                    log.debug("Policy filter: path rejected — {} flights > max_flights {}",
                            flightCount, routeConstraints.getMaxFlights());
                    continue;
                }
            }

            // ── Per-hub: first-mile constraint ──
            // Origin hub = the airport where the first FLIGHT departs from
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
            // Dest hub = the airport where the last FLIGHT arrives at
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

            // Path passed all policy checks
            filtered.add(path);
        }

        return filtered;
    }

    /* ═══════════════════════════════════════════════
       Path → JourneyResult Conversion
       ═══════════════════════════════════════════════ */
    private JourneyResult buildResult(List<ResolvedEdge> path, LocalDate date, String targetCurrency) {
        if (path.isEmpty()) return null;

        List<JourneySegment> segments = new ArrayList<>();
        int totalCost = 0;
        int totalCO2 = 0;

        for (ResolvedEdge edge : path) {
            int originalCostCents = edge.costCents() != null ? edge.costCents() : 0;
            String segCurrency = edge.currency() != null ? edge.currency() : "EUR";

            // Convert segment cost to the target display currency
            int convertedCost = CurrencyConverter.convert(originalCostCents, segCurrency, targetCurrency);

            segments.add(JourneySegment.builder()
                    .mode(edge.transportModeCode())
                    .originCode(edge.origin().iataCode() != null ? edge.origin().iataCode() : shortCode(edge.origin().name()))
                    .originName(edge.origin().name())
                    .destinationCode(edge.destination().iataCode() != null ? edge.destination().iataCode() : shortCode(edge.destination().name()))
                    .destinationName(edge.destination().name())
                    .departureTime(edge.departureTime() != null ? edge.departureTime().toString().substring(0, 5) : "")
                    .arrivalTime(edge.arrivalTime() != null ? edge.arrivalTime().toString().substring(0, 5) : "")
                    .durationMin(edge.durationMin())
                    .serviceCode(edge.serviceCode())
                    .provider(edge.providerCode())
                    .costCents(convertedCost)
                    .currency(targetCurrency)
                    .edgeId(edge.id() != null ? edge.id().toString() : null)
                    .originTimezone(edge.origin().timezone())
                    .destinationTimezone(edge.destination().timezone())
                    .build());

            totalCost += convertedCost;
            if (edge.co2Grams() != null) totalCO2 += edge.co2Grams();
        }

        // Flight-only duration + extract first/last flight times
        int flightDuration = 0;
        String firstFlightDep = null;
        String lastFlightArr = null;
        String depTimezone = null;
        String arrTimezone = null;
        String depCode = null;
        String arrCode = null;

        for (ResolvedEdge edge : path) {
            if ("FLIGHT".equals(edge.transportModeCode())) {
                flightDuration += edge.durationMin();
                if (firstFlightDep == null && edge.departureTime() != null) {
                    firstFlightDep = edge.departureTime().toString().substring(0, 5);
                    depTimezone = edge.origin().timezone();
                    depCode = edge.origin().iataCode() != null ? edge.origin().iataCode() : shortCode(edge.origin().name());
                }
                if (edge.arrivalTime() != null) {
                    lastFlightArr = edge.arrivalTime().toString().substring(0, 5);
                    arrTimezone = edge.destination().timezone();
                    arrCode = edge.destination().iataCode() != null ? edge.destination().iataCode() : shortCode(edge.destination().name());
                }
            }
        }

        // If no flights found (shouldn't happen after policy filter), sum all
        if (flightDuration == 0) {
            for (ResolvedEdge edge : path) {
                flightDuration += edge.durationMin();
            }
        }

        return JourneyResult.builder()
                .id(UUID.randomUUID().toString())
                .segments(segments)
                .totalDurationMin(flightDuration)
                .totalCostCents(totalCost)
                .currency(targetCurrency)
                .co2Grams(totalCO2)
                .transfers(segments.size() - 1)
                .tags(new ArrayList<>())
                .departureTime(firstFlightDep)
                .arrivalTime(lastFlightArr)
                .departureTimezone(depTimezone)
                .arrivalTimezone(arrTimezone)
                .departureCode(depCode)
                .arrivalCode(arrCode)
                .build();
    }

    /* ═══════════════════════════════════════════════
       Ranking & Labeling (unchanged)
       ═══════════════════════════════════════════════ */
    private List<JourneyResult> rankAndLabel(List<JourneyResult> results, String sortBy) {
        if (results.isEmpty()) return results;

        Comparator<JourneyResult> comparator = switch (sortBy != null ? sortBy : "FASTEST") {
            case "CHEAPEST" -> Comparator.comparingInt(JourneyResult::getTotalCostCents);
            case "GREENEST" -> Comparator.comparingInt(JourneyResult::getCo2Grams);
            case "FEWEST_TRANSFERS" -> Comparator.comparingInt(JourneyResult::getTransfers);
            default -> Comparator.comparingInt(JourneyResult::getTotalDurationMin);
        };

        results.sort(comparator);

        // Deduplicate
        List<JourneyResult> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JourneyResult r : results) {
            String key = r.getSegments().stream()
                    .map(s -> (s.getEdgeId() != null ? s.getEdgeId() : s.getMode()) + ":" + s.getDepartureTime())
                    .collect(Collectors.joining("|"));
            if (seen.add(key)) unique.add(r);
        }

        log.info("Ranking: {} total unique paths", unique.size());

        // Ensure modal diversity: reserve slots for routes with unique mode combinations
        // so transit routes (TRAIN, SUBWAY, BUS) aren't pushed out by TAXI/UBER-only routes
        List<JourneyResult> top = new ArrayList<>();
        Set<String> diverseModes = Set.of("TRAIN", "SUBWAY", "BUS", "FERRY", "TRAM");
        Map<String, JourneyResult> bestByModeSet = new LinkedHashMap<>();

        for (JourneyResult r : unique) {
            // Build a mode-set key like "TRAIN+FLIGHT+TAXI" to identify unique mode combos
            String modeSetKey = r.getSegments().stream()
                    .map(JourneySegment::getMode)
                    .collect(Collectors.joining("+"));
            boolean hasDiverseMode = r.getSegments().stream()
                    .anyMatch(s -> diverseModes.contains(s.getMode()));
            if (hasDiverseMode) {
                bestByModeSet.putIfAbsent(modeSetKey, r); // keep best (already sorted)
            } else {
                if (top.size() < MAX_RESULTS) top.add(r);
            }
        }

        // Insert best route for each unique diverse mode-set (up to 5 slots)
        int diverseSlots = 0;
        for (JourneyResult dr : bestByModeSet.values()) {
            if (diverseSlots >= 5) break;
            if (top.size() < MAX_RESULTS) {
                top.add(dr);
            } else {
                top.set(MAX_RESULTS - 1 - diverseSlots, dr);
            }
            diverseSlots++;
        }

        // Re-sort after diversity injection
        top.sort(comparator);
        log.info("Final top: {} results ({} diverse mode-sets from {} unique combos)",
                top.size(), diverseSlots, bestByModeSet.size());

        if (!top.isEmpty()) assignLabels(top);
        return top;
    }

    private void assignLabels(List<JourneyResult> results) {
        JourneyResult fastest = results.stream().min(Comparator.comparingInt(JourneyResult::getTotalDurationMin)).orElse(null);
        JourneyResult cheapest = results.stream().min(Comparator.comparingInt(JourneyResult::getTotalCostCents)).orElse(null);
        JourneyResult greenest = results.stream().min(Comparator.comparingInt(JourneyResult::getCo2Grams)).orElse(null);
        JourneyResult fewest = results.stream().min(Comparator.comparingInt(JourneyResult::getTransfers)).orElse(null);

        for (JourneyResult r : results) {
            List<String> tags = new ArrayList<>();
            if (r == fastest) { r.setLabel("En Hızlı"); tags.add("fastest"); }
            if (r == cheapest) { if (r.getLabel() == null) r.setLabel("En Ucuz"); tags.add("cheapest"); }
            if (r == greenest) { if (r.getLabel() == null) r.setLabel("En Yeşil"); tags.add("greenest"); }
            if (r == fewest) { if (r.getLabel() == null) r.setLabel("En Az Aktarma"); tags.add("fewest_transfers"); }
            if (r.getLabel() == null) r.setLabel("Alternatif " + (results.indexOf(r) + 1));
            r.setTags(tags);
        }

        if (!results.isEmpty() && results.get(0).getTags() != null) {
            results.get(0).getTags().add("recommended");
        }
    }

    /* ═══════════════════════════════════════════════
       Helpers
       ═══════════════════════════════════════════════ */

    private String locKey(ResolvedLocation loc) {
        if (loc.id() != null) return loc.id().toString();
        return String.format("%.4f,%.4f", loc.lat(), loc.lon());
    }

    private void indexLocation(Map<String, ResolvedLocation> index, ResolvedLocation loc) {
        index.putIfAbsent(locKey(loc), loc);
    }

    private void addEdges(Map<String, List<ResolvedEdge>> adjacency, String fromKey,
                          List<ResolvedEdge> edges, Map<String, ResolvedLocation> locationIndex) {
        if (edges == null || edges.isEmpty()) return;
        adjacency.computeIfAbsent(fromKey, k -> new ArrayList<>()).addAll(edges);
        for (ResolvedEdge edge : edges) {
            indexLocation(locationIndex, edge.origin());
            indexLocation(locationIndex, edge.destination());
        }
    }

    private boolean isLocationInPath(List<ResolvedEdge> path, String locationKey) {
        for (ResolvedEdge edge : path) {
            if (locKey(edge.origin()).equals(locationKey)) return true;
            if (locKey(edge.destination()).equals(locationKey)) return true;
        }
        return false;
    }


    private ResolvedLocation toResolvedLocation(Location loc) {
        return ResolvedLocation.fromDb(
                loc.getId(), loc.getName(), loc.getIataCode(),
                loc.getLat() != null ? loc.getLat().doubleValue() : 0,
                loc.getLon() != null ? loc.getLon().doubleValue() : 0,
                loc.getType() != null ? loc.getType().getValue() : "AIRPORT",
                loc.getCountryIsoCode(),
                loc.getTimezone()
        );
    }

    private String shortCode(String name) {
        if (name == null) return "???";
        return name.length() > 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase();
    }

    private double haversineM(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6_371_000 * c;
    }
}
