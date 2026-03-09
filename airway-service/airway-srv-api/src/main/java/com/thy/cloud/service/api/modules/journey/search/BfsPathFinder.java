package com.thy.cloud.service.api.modules.journey.search;

import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static com.thy.cloud.service.api.modules.journey.graph.HubDiscoveryService.locKey;

/**
 * Time-aware BFS path finder over the resolved adjacency graph.
 * Pure algorithm — no DB access, no side effects.
 */
@Slf4j
@Component
public class BfsPathFinder {

    private static final int MAX_BFS_ITERATIONS = 3000;
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final int ENOUGH_PATHS = 50;

    /**
     * Find all valid paths from originKey to any destinationKey using BFS.
     *
     * @param adjacency         the resolved adjacency graph
     * @param originKey         locKey of the starting node
     * @param destinationKeys   locKeys of acceptable destination nodes
     * @param date              travel date (for context, not used in BFS itself)
     * @param earliestDeparture earliest departure time at origin
     * @param maxTransfers      max number of transfers (path length = maxTransfers + 1)
     * @param maxDurationMinutes max total journey duration
     * @param transferTimes     mode → transfer time (minutes) map
     * @param completePaths     output list — paths are added here
     */
    public void findPaths(
            Map<String, List<ResolvedEdge>> adjacency,
            String originKey, Set<String> destinationKeys,
            LocalTime earliestDeparture,
            int maxTransfers, int maxDurationMinutes,
            Map<String, Integer> transferTimes,
            List<List<ResolvedEdge>> completePaths
    ) {
        record BfsState(String locationKey, List<ResolvedEdge> path,
                        LocalTime earliestDep, int totalMinutes) {}

        Queue<BfsState> queue = new LinkedList<>();
        queue.add(new BfsState(originKey, new ArrayList<>(), earliestDeparture, 0));

        Map<String, Integer> bestDepth = new HashMap<>();
        bestDepth.put(originKey, 0);

        int iterations = 0;
        while (!queue.isEmpty()) {
            if (++iterations > MAX_BFS_ITERATIONS) {
                log.warn("BFS iteration limit reached ({}) — returning {} paths", MAX_BFS_ITERATIONS, completePaths.size());
                break;
            }
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

                // Visited-depth pruning
                int newDepth = state.path.size() + 1;
                Integer prev = bestDepth.get(destKey);
                if (prev != null && prev + 1 < newDepth && !destinationKeys.contains(destKey)) continue;
                if (prev == null || newDepth < prev) bestDepth.put(destKey, newDepth);

                // Time compatibility
                LocalTime depTime = edge.departureTime();
                LocalTime arrTime = edge.arrivalTime();

                if (depTime != null && arrTime != null) {
                    // Fixed-schedule edge
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
                        int transferMin = transferTimes.getOrDefault(edge.transportModeCode(), 10);
                        queue.add(new BfsState(destKey, newPath,
                                arrTime.plusMinutes(transferMin), newTotal + transferMin));
                    }
                } else {
                    // On-demand/computed edge
                    int dur = edge.durationMin() > 0 ? edge.durationMin() : 15;
                    LocalTime dep = state.earliestDep;
                    LocalTime arr = dep.plusMinutes(dur);
                    int newTotal = state.totalMinutes + dur;
                    if (newTotal > maxDurationMinutes) continue;

                    ResolvedEdge timedEdge = edge.withTimes(dep, arr, dur);

                    List<ResolvedEdge> newPath = new ArrayList<>(state.path);
                    newPath.add(timedEdge);

                    if (destinationKeys.contains(destKey)) {
                        completePaths.add(newPath);
                    } else if (newPath.size() <= maxTransfers && queue.size() < MAX_QUEUE_SIZE) {
                        int transferMin = transferTimes.getOrDefault(edge.transportModeCode(), 10);
                        queue.add(new BfsState(destKey, newPath,
                                arr.plusMinutes(transferMin), newTotal + transferMin));
                    }
                }
            }
        }
    }

    private boolean isLocationInPath(List<ResolvedEdge> path, String locationKey) {
        for (ResolvedEdge edge : path) {
            if (locKey(edge.origin()).equals(locationKey)) return true;
            if (locKey(edge.destination()).equals(locationKey)) return true;
        }
        return false;
    }
}
