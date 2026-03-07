package com.thy.cloud.service.api.resolver.edge;

import com.thy.cloud.service.api.resolver.model.EdgeSearchContext;
import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import com.thy.cloud.service.dao.entity.transport.TransportMode;
import com.thy.cloud.service.dao.enums.EnumEdgeResolution;
import com.thy.cloud.service.dao.repository.transport.TransportModeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes ephemeral edges for walking and biking based on Haversine distance.
 * <p>
 * These edges are NEVER stored in the database — they are calculated at query time
 * based on the distance between two points and the configured speed in {@code transport_mode}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ComputedEdgeResolver implements EdgeResolver {

    private final TransportModeRepository transportModeRepository;

    private static final double EARTH_RADIUS_M = 6_371_000;

    @Override
    public String getResolution() {
        return "COMPUTED";
    }

    @Override
    public List<ResolvedEdge> resolve(ResolvedLocation origin,
                                       ResolvedLocation destination,
                                       EdgeSearchContext context) {
        if (origin == null || destination == null) {
            return List.of();
        }

        double distanceM = haversine(origin.lat(), origin.lon(), destination.lat(), destination.lon());
        List<ResolvedEdge> edges = new ArrayList<>();

        // Get computed transport modes (WALKING, BIKE)
        List<TransportMode> computedModes = transportModeRepository
                .findByEdgeResolutionAndIsActiveTrue(EnumEdgeResolution.COMPUTED);

        for (TransportMode mode : computedModes) {
            if (!context.isModeAllowed(mode.getCode())) {
                continue;
            }

            // Check max distance (walking has a limit, biking has a longer one)
            Integer maxAccessM = mode.getMaxWalkingAccessM();
            if (maxAccessM != null && maxAccessM > 0 && distanceM > maxAccessM) {
                log.debug("Distance {}m exceeds max {}m for mode {}",
                        (int) distanceM, maxAccessM, mode.getCode());
                continue;
            }

            // Calculate duration based on configured speed
            int speedKmh = mode.getDefaultSpeedKmh() != null ? mode.getDefaultSpeedKmh() : 5;
            int durationMin = (int) Math.ceil((distanceM / 1000.0) / speedKmh * 60);

            // CO₂ is zero for pedestrian modes
            edges.add(ResolvedEdge.ephemeral(
                    origin, destination,
                    mode.getCode(),
                    "COMPUTED",
                    durationMin,
                    (int) distanceM,
                    0,     // free
                    0      // zero emissions
            ));

            log.debug("ComputedEdge: {} → {} via {} = {}m, {}min",
                    origin.name(), destination.name(), mode.getCode(),
                    (int) distanceM, durationMin);
        }

        return edges;
    }

    /**
     * Haversine formula: great-circle distance between two points on Earth.
     */
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }
}
