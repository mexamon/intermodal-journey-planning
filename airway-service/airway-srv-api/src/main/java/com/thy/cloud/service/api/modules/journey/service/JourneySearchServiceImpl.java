package com.thy.cloud.service.api.modules.journey.service;

import com.thy.cloud.service.api.modules.journey.model.JourneyResult;
import com.thy.cloud.service.api.modules.journey.model.JourneySearchRequest;
import com.thy.cloud.service.api.modules.journey.model.JourneySegment;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.entity.transport.EdgeTrip;
import com.thy.cloud.service.dao.entity.transport.TransportationEdge;
import com.thy.cloud.service.dao.enums.EnumEdgeStatus;
import com.thy.cloud.service.dao.repository.inventory.LocationRepository;
import com.thy.cloud.service.dao.repository.transport.TransportationEdgeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final TransportationEdgeRepository edgeRepository;
    private final LocationRepository locationRepository;

    // Transfer times (minutes) per mode arriving at
    private static final Map<String, Integer> TRANSFER_TIMES = Map.of(
            "FLIGHT", 60,    // need 1h for check-in after arriving at airport
            "TRAIN", 15,
            "BUS", 10,
            "SUBWAY", 5,
            "FERRY", 15,
            "WALKING", 0,
            "UBER", 5
    );

    private static final int MAX_RESULTS = 10;

    @Override
    public List<JourneyResult> search(JourneySearchRequest request) {
        // 1. Resolve origin & destination
        Location origin = resolveLocation(request.getOriginLocationId(), request.getOriginIataCode());
        Location destination = resolveLocation(request.getDestinationLocationId(), request.getDestinationIataCode());

        if (origin.getId().equals(destination.getId())) {
            return Collections.emptyList();
        }

        LocalDate date = request.getDepartureDate() != null ? request.getDepartureDate() : LocalDate.now();
        LocalTime earliest = request.getEarliestDeparture() != null ? request.getEarliestDeparture() : LocalTime.of(0, 0);
        int maxTransfers = Math.min(request.getMaxTransfers(), 6);
        int maxDuration = request.getMaxDurationMinutes();

        // 2. Build bounded adjacency map (only edges reachable from origin within maxTransfers hops)
        Map<UUID, List<TransportationEdge>> adjacency = buildAdjacencyMap(
                origin.getId(), maxTransfers, request.getPreferredModes());

        // 3. BFS — find all paths from origin to destination
        List<List<PathStep>> completePaths = new ArrayList<>();
        bfs(adjacency, origin.getId(), destination.getId(), date, earliest,
                maxTransfers, maxDuration, completePaths);

        // 4. Convert paths to JourneyResults
        List<JourneyResult> results = completePaths.stream()
                .map(path -> buildResult(path, date))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 5. Sort & label
        return rankAndLabel(results, request.getSortBy());
    }

    /* ═══════════════════════════════════════════════
       Location Resolution
       ═══════════════════════════════════════════════ */
    private Location resolveLocation(UUID locationId, String iataCode) {
        if (locationId != null) {
            return locationRepository.findById(locationId)
                    .orElseThrow(() -> new EntityNotFoundException("Location not found: " + locationId));
        }
        if (iataCode != null && !iataCode.isBlank()) {
            return locationRepository.findByIataCode(iataCode.toUpperCase().trim())
                    .orElseThrow(() -> new EntityNotFoundException("Location not found for IATA: " + iataCode));
        }
        throw new IllegalArgumentException("Either locationId or iataCode must be provided");
    }

    /* ═══════════════════════════════════════════════
       Graph Construction — Bounded hop-by-hop loading
       ═══════════════════════════════════════════════ */
    private Map<UUID, List<TransportationEdge>> buildAdjacencyMap(
            UUID originId, int maxTransfers, List<String> preferredModes) {

        Map<UUID, List<TransportationEdge>> adjacency = new HashMap<>();
        Set<UUID> visited = new HashSet<>();
        Set<UUID> frontier = new HashSet<>();
        frontier.add(originId);

        Set<String> modes = (preferredModes != null && !preferredModes.isEmpty())
                ? new HashSet<>(preferredModes) : null;

        // Load edges hop by hop (origin → 1st hop → 2nd hop → ... up to maxTransfers)
        for (int hop = 0; hop <= maxTransfers; hop++) {
            if (frontier.isEmpty()) break;

            for (UUID locId : frontier) {
                if (visited.contains(locId)) continue;
                visited.add(locId);

                List<TransportationEdge> edges = edgeRepository.findByOriginLocationIdAndStatus(
                        locId, EnumEdgeStatus.ACTIVE);

                // Mode filter
                if (modes != null) {
                    edges = edges.stream()
                            .filter(e -> modes.contains(e.getTransportMode().getCode()))
                            .collect(Collectors.toList());
                }

                adjacency.put(locId, edges);
            }

            // Next frontier = all destinations of current edges not yet visited
            Set<UUID> nextFrontier = new HashSet<>();
            for (UUID locId : frontier) {
                List<TransportationEdge> edges = adjacency.getOrDefault(locId, Collections.emptyList());
                for (TransportationEdge edge : edges) {
                    UUID destId = edge.getDestinationLocation().getId();
                    if (!visited.contains(destId)) {
                        nextFrontier.add(destId);
                    }
                }
            }
            frontier = nextFrontier;
        }

        return adjacency;
    }

    /* ═══════════════════════════════════════════════
       BFS Traversal with Time Awareness
       ═══════════════════════════════════════════════ */
    private record PathStep(
            TransportationEdge edge,
            EdgeTrip trip,          // null for FREQUENCY/ON_DEMAND
            LocalTime departure,
            LocalTime arrival,
            int durationMin,
            int costCents
    ) {}

    private void bfs(
            Map<UUID, List<TransportationEdge>> adjacency,
            UUID originId, UUID destinationId,
            LocalDate date, LocalTime earliestDeparture,
            int maxTransfers, int maxDurationMinutes,
            List<List<PathStep>> completePaths
    ) {
        // BFS queue: (current location, path so far, earliest possible departure, total minutes)
        record BfsState(UUID locationId, List<PathStep> path, LocalTime earliestDep, int totalMinutes) {}

        Queue<BfsState> queue = new LinkedList<>();
        queue.add(new BfsState(originId, new ArrayList<>(), earliestDeparture, 0));

        int dayBit = dayOfWeekBit(date);

        while (!queue.isEmpty() && completePaths.size() < MAX_RESULTS * 3) {
            BfsState state = queue.poll();

            // Prune: too many transfers
            if (state.path.size() > maxTransfers + 1) continue;
            // Prune: too long
            if (state.totalMinutes > maxDurationMinutes) continue;

            List<TransportationEdge> outgoing = adjacency.getOrDefault(state.locationId, Collections.emptyList());

            for (TransportationEdge edge : outgoing) {
                UUID destId = edge.getDestinationLocation().getId();

                // Avoid cycles: don't revisit locations already in path
                if (isLocationInPath(state.path, destId)) continue;

                List<PathStep> departures = resolveDepartures(edge, date, dayBit, state.earliestDep);

                for (PathStep step : departures) {
                    int newTotal = state.totalMinutes + step.durationMin;
                    if (newTotal > maxDurationMinutes) continue;

                    List<PathStep> newPath = new ArrayList<>(state.path);
                    newPath.add(step);

                    if (destId.equals(destinationId)) {
                        // Reached destination!
                        completePaths.add(newPath);
                    } else if (newPath.size() <= maxTransfers) {
                        // Continue search: add transfer time
                        String modeCode = edge.getTransportMode().getCode();
                        int transferMin = TRANSFER_TIMES.getOrDefault(modeCode, 10);
                        LocalTime nextEarliest = step.arrival.plusMinutes(transferMin);

                        queue.add(new BfsState(destId, newPath, nextEarliest,
                                newTotal + transferMin));
                    }
                }
            }
        }
    }

    /* ═══════════════════════════════════════════════
       Departure Resolution (per schedule type)
       ═══════════════════════════════════════════════ */
    private List<PathStep> resolveDepartures(TransportationEdge edge, LocalDate date, int dayBit, LocalTime earliest) {
        List<PathStep> result = new ArrayList<>();
        String scheduleType = edge.getScheduleType() != null ? edge.getScheduleType().getValue() : "FIXED";

        switch (scheduleType) {
            case "FIXED":
                // Use edge_trip table
                List<EdgeTrip> trips = edge.getTrips();
                if (trips != null) {
                    for (EdgeTrip trip : trips) {
                        // Check operating day
                        if ((trip.getOperatingDaysMask() & dayBit) == 0) continue;
                        // Check validity window
                        if (trip.getValidFrom() != null && date.isBefore(trip.getValidFrom())) continue;
                        if (trip.getValidTo() != null && date.isAfter(trip.getValidTo())) continue;
                        // Check departure time
                        if (trip.getDepartureTime().isBefore(earliest)) continue;

                        int dur = (int) trip.getDepartureTime().until(trip.getArrivalTime(), ChronoUnit.MINUTES);
                        if (dur <= 0) dur += 24 * 60; // overnight

                        result.add(new PathStep(
                                edge, trip,
                                trip.getDepartureTime(), trip.getArrivalTime(),
                                dur,
                                trip.getEstimatedCostCents() != null ? trip.getEstimatedCostCents() : 0
                        ));
                    }
                }
                break;

            case "FREQUENCY":
                // Synthetic departures every N minutes
                if (edge.getFrequencyMinutes() != null && edge.getFrequencyMinutes() > 0) {
                    if ((edge.getOperatingDaysMask() & dayBit) == 0) break;

                    LocalTime opStart = edge.getOperatingStartTime() != null ? edge.getOperatingStartTime() : LocalTime.of(5, 0);
                    LocalTime opEnd = edge.getOperatingEndTime() != null ? edge.getOperatingEndTime() : LocalTime.of(23, 0);
                    int freq = edge.getFrequencyMinutes();
                    int duration = edge.getEstimatedDurationMin() != null ? edge.getEstimatedDurationMin() : 15;

                    // Find next departure >= earliest within operating window
                    LocalTime dep = opStart;
                    while (dep.isBefore(earliest) && dep.isBefore(opEnd)) {
                        dep = dep.plusMinutes(freq);
                    }

                    // Only add ONE departure (the next one) for FREQUENCY to avoid path explosion
                    if (!dep.isAfter(opEnd)) {
                        LocalTime arr = dep.plusMinutes(duration);
                        result.add(new PathStep(edge, null, dep, arr, duration, 0));
                    }
                }
                break;

            case "ON_DEMAND":
                // Always available, depart at earliest
                if ((edge.getOperatingDaysMask() & dayBit) == 0) break;
                int duration = edge.getEstimatedDurationMin() != null ? edge.getEstimatedDurationMin() : 30;
                LocalTime dep = earliest.isBefore(LocalTime.of(4, 0)) ? LocalTime.of(4, 0) : earliest;
                LocalTime arr = dep.plusMinutes(duration);
                result.add(new PathStep(edge, null, dep, arr, duration, 0));
                break;
        }

        return result;
    }

    /* ═══════════════════════════════════════════════
       Path → JourneyResult Conversion
       ═══════════════════════════════════════════════ */
    private JourneyResult buildResult(List<PathStep> path, LocalDate date) {
        if (path.isEmpty()) return null;

        List<JourneySegment> segments = new ArrayList<>();
        int totalCost = 0;
        int totalCO2 = 0;

        for (PathStep step : path) {
            TransportationEdge edge = step.edge();
            String modeCode = edge.getTransportMode().getCode();
            String providerCode = edge.getProvider() != null ? edge.getProvider().getCode() : null;

            segments.add(JourneySegment.builder()
                    .mode(modeCode)
                    .originCode(getLocationCode(edge.getOriginLocation()))
                    .originName(edge.getOriginLocation().getName())
                    .destinationCode(getLocationCode(edge.getDestinationLocation()))
                    .destinationName(edge.getDestinationLocation().getName())
                    .departureTime(step.departure().toString().substring(0, 5))
                    .arrivalTime(step.arrival().toString().substring(0, 5))
                    .durationMin(step.durationMin())
                    .serviceCode(step.trip() != null ? step.trip().getServiceCode() : null)
                    .provider(providerCode)
                    .costCents(step.costCents())
                    .edgeId(edge.getId().toString())
                    .tripId(step.trip() != null ? step.trip().getId().toString() : null)
                    .build());

            totalCost += step.costCents();
            if (edge.getCo2Grams() != null) totalCO2 += edge.getCo2Grams();
        }

        // Total duration = first departure to last arrival
        PathStep first = path.get(0);
        PathStep last = path.get(path.size() - 1);
        int totalDuration = (int) first.departure().until(last.arrival(), ChronoUnit.MINUTES);
        if (totalDuration <= 0) totalDuration += 24 * 60;

        return JourneyResult.builder()
                .id(UUID.randomUUID().toString())
                .segments(segments)
                .totalDurationMin(totalDuration)
                .totalCostCents(totalCost)
                .co2Grams(totalCO2)
                .transfers(segments.size() - 1)
                .tags(new ArrayList<>())
                .build();
    }

    /* ═══════════════════════════════════════════════
       Ranking & Labeling
       ═══════════════════════════════════════════════ */
    private List<JourneyResult> rankAndLabel(List<JourneyResult> results, String sortBy) {
        if (results.isEmpty()) return results;

        // Sort by primary criteria
        Comparator<JourneyResult> comparator = switch (sortBy != null ? sortBy : "FASTEST") {
            case "CHEAPEST" -> Comparator.comparingInt(JourneyResult::getTotalCostCents);
            case "GREENEST" -> Comparator.comparingInt(JourneyResult::getCo2Grams);
            case "FEWEST_TRANSFERS" -> Comparator.comparingInt(JourneyResult::getTransfers);
            default -> Comparator.comparingInt(JourneyResult::getTotalDurationMin);
        };

        results.sort(comparator);

        // Deduplicate (same segments = same journey)
        List<JourneyResult> unique = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JourneyResult r : results) {
            String key = r.getSegments().stream()
                    .map(s -> s.getEdgeId() + ":" + s.getDepartureTime())
                    .collect(Collectors.joining("|"));
            if (seen.add(key)) {
                unique.add(r);
            }
        }

        // Take top N
        List<JourneyResult> top = unique.stream().limit(MAX_RESULTS).collect(Collectors.toList());

        // Label
        if (!top.isEmpty()) assignLabels(top);

        return top;
    }

    private void assignLabels(List<JourneyResult> results) {
        // Find best in each category
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

        // Mark first as recommended
        if (!results.isEmpty() && results.get(0).getTags() != null) {
            results.get(0).getTags().add("recommended");
        }
    }

    /* ═══════════════════════════════════════════════
       Helpers
       ═══════════════════════════════════════════════ */
    private int dayOfWeekBit(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return 1 << (dow.getValue() - 1); // Mon=bit0 ... Sun=bit6
    }

    private boolean isLocationInPath(List<PathStep> path, UUID locationId) {
        for (PathStep step : path) {
            if (step.edge().getOriginLocation().getId().equals(locationId)) return true;
            if (step.edge().getDestinationLocation().getId().equals(locationId)) return true;
        }
        return false;
    }

    private String getLocationCode(Location location) {
        if (location.getIataCode() != null && !location.getIataCode().isBlank()) {
            return location.getIataCode();
        }
        return location.getName().length() > 3 ? location.getName().substring(0, 3).toUpperCase() : location.getName().toUpperCase();
    }
}
