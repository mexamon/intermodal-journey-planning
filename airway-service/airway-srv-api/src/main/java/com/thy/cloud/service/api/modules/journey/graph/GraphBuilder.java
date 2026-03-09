package com.thy.cloud.service.api.modules.journey.graph;

import com.thy.cloud.service.api.resolver.edge.EdgeResolver;
import com.thy.cloud.service.api.resolver.model.EdgeSearchContext;
import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.thy.cloud.service.api.modules.journey.graph.HubDiscoveryService.locKey;

/**
 * Builds the adjacency graph used by BFS path finding.
 * Three phases: First-mile (A), Trunk (B), Last-mile (C/C.2).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GraphBuilder {

    private final List<EdgeResolver> edgeResolvers;

    /**
     * Phase A: First-mile edges — origin → each origin hub via COMPUTED/GTFS.
     */
    public void buildFirstMile(ResolvedLocation origin, List<ResolvedLocation> hubs,
                                EdgeSearchContext context,
                                Map<String, List<ResolvedEdge>> adjacency,
                                Map<String, ResolvedLocation> locationIndex) {
        log.info("Phase A: {} origin hubs for first-mile from '{}'", hubs.size(), origin.name());
        for (ResolvedLocation hub : hubs) {
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
                        if ("GTFS_LIVE".equals(res)) {
                            edges = remapGtfsEdges(edges, hub);
                        }
                        log.info("  → {} first-mile edges from {} via {}", edges.size(), hub.name(), res);
                        addEdges(adjacency, locKey(origin), edges, locationIndex);
                    } catch (Exception e) {
                        log.warn("First-mile {} failed: {}", res, e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Phase B: Trunk — BFS expansion using STATIC + AMADEUS resolvers only.
     */
    public void buildTrunk(List<ResolvedLocation> originHubs, boolean originIsHub,
                            ResolvedLocation origin, EdgeSearchContext context, int maxTransfers,
                            Map<String, List<ResolvedEdge>> adjacency,
                            Map<String, ResolvedLocation> locationIndex) {
        log.info("Phase B: Trunk BFS starting from {} hubs, originIsHub={}", originHubs.size(), originIsHub);

        Set<String> visited = new HashSet<>();
        Set<String> frontier = new HashSet<>();

        if (originIsHub) {
            frontier.add(locKey(origin));
        }
        for (ResolvedLocation hub : originHubs) {
            frontier.add(locKey(hub));
        }

        // IATA→ResolvedLocation map for matching virtual destinations to persisted hubs
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

                for (EdgeResolver resolver : edgeResolvers) {
                    String res = resolver.getResolution();
                    if (!"STATIC".equals(res) && !"AMADEUS".equals(res) && !"AMADEUS_LIVE".equals(res)) continue;

                    try {
                        List<ResolvedEdge> edges = resolver.resolve(loc, null, context);
                        List<ResolvedEdge> resolvedEdges = resolveVirtualDestinations(edges, iataIndex);

                        addEdges(adjacency, locKeyStr, resolvedEdges, locationIndex);

                        for (ResolvedEdge edge : resolvedEdges) {
                            String dKey = locKey(edge.destination());
                            indexLocation(locationIndex, edge.destination());
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

    /**
     * Phase C + C.2: Last-mile edges — dest hubs → destination via COMPUTED/GTFS/STATIC.
     * Also extends from intermediate stations to final destination.
     */
    public void buildLastMile(List<ResolvedLocation> destHubs, ResolvedLocation destination,
                               EdgeSearchContext context,
                               Map<String, List<ResolvedEdge>> adjacency,
                               Map<String, ResolvedLocation> locationIndex) {
        // ── Phase C: dest hub → destination ──
        log.info("Phase C: {} destination hubs to process", destHubs.size());
        for (ResolvedLocation hub : destHubs) {
            if (locKey(hub).equals(locKey(destination))) continue;
            for (EdgeResolver resolver : edgeResolvers) {
                String res = resolver.getResolution();
                if ("COMPUTED".equals(res) || "GTFS_LIVE".equals(res) || "STATIC".equals(res)) {
                    try {
                        ResolvedLocation resolverDest = "STATIC".equals(res) ? null : destination;
                        List<ResolvedEdge> edges = resolver.resolve(hub, resolverDest, context);

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

        // ── Phase C.2: Extend from intermediate STATIONS to destination ──
        Set<String> intermediateKeys = new HashSet<>();
        for (ResolvedLocation hub : destHubs) {
            List<ResolvedEdge> hubEdges = adjacency.getOrDefault(locKey(hub), List.of());
            for (ResolvedEdge edge : hubEdges) {
                String destEdgeKey = locKey(edge.destination());
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
    }

    // ── Internal helpers ──

    private List<ResolvedEdge> remapGtfsEdges(List<ResolvedEdge> edges, ResolvedLocation hub) {
        List<ResolvedEdge> remapped = new ArrayList<>();
        for (ResolvedEdge edge : edges) {
            double dist = HubDiscoveryService.haversineM(
                    edge.destination().lat(), edge.destination().lon(),
                    hub.lat(), hub.lon());
            if (dist < 2000) {
                remapped.add(edge.withDestination(hub));
            } else {
                remapped.add(edge);
            }
        }
        return remapped;
    }

    private List<ResolvedEdge> resolveVirtualDestinations(List<ResolvedEdge> edges,
                                                           Map<String, ResolvedLocation> iataIndex) {
        List<ResolvedEdge> resolved = new ArrayList<>();
        for (ResolvedEdge edge : edges) {
            ResolvedLocation dest = edge.destination();
            if (!dest.persisted() && dest.iataCode() != null && iataIndex.containsKey(dest.iataCode())) {
                resolved.add(edge.withDestination(iataIndex.get(dest.iataCode())));
            } else if (!dest.persisted() && dest.name() != null) {
                String possibleIata = dest.name().replace(" Airport", "").trim();
                if (possibleIata.length() == 3 && iataIndex.containsKey(possibleIata)) {
                    resolved.add(edge.withDestination(iataIndex.get(possibleIata)));
                } else {
                    resolved.add(edge);
                }
            } else {
                resolved.add(edge);
            }
        }
        return resolved;
    }

    public static void addEdges(Map<String, List<ResolvedEdge>> adjacency, String fromKey,
                                 List<ResolvedEdge> edges, Map<String, ResolvedLocation> locationIndex) {
        if (edges == null || edges.isEmpty()) return;
        adjacency.computeIfAbsent(fromKey, k -> new ArrayList<>()).addAll(edges);
        for (ResolvedEdge edge : edges) {
            indexLocation(locationIndex, edge.origin());
            indexLocation(locationIndex, edge.destination());
        }
    }

    public static void indexLocation(Map<String, ResolvedLocation> index, ResolvedLocation loc) {
        index.putIfAbsent(locKey(loc), loc);
    }
}
