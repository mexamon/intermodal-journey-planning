package com.thy.cloud.service.api.resolver.edge;

import com.thy.cloud.service.api.resolver.model.EdgeSearchContext;
import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import com.thy.cloud.service.dao.entity.transport.TransportMode;
import com.thy.cloud.service.dao.entity.transport.TransportServiceArea;
import com.thy.cloud.service.dao.enums.EnumEdgeResolution;
import com.thy.cloud.service.dao.repository.transport.TransportModeRepository;
import com.thy.cloud.service.dao.repository.transport.TransportServiceAreaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Computes ephemeral edges for walking, biking, taxi, and uber based on Haversine distance.
 * <p>
 * These edges are NEVER stored in the database — they are calculated at query time.
 * <p>
 * Service-area-aware: When a matching {@code transport_service_area} exists for the
 * origin/destination coordinates, its {@code config_json} overrides the global
 * {@code transport_mode.max_walking_access_m} limit and provides pricing info.
 * If no service area matches, the global defaults are used as fallback.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ComputedEdgeResolver implements EdgeResolver {

    private final TransportModeRepository transportModeRepository;
    private final TransportServiceAreaRepository serviceAreaRepository;
    private static final ObjectMapper mapper = new ObjectMapper();

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

        // Get computed transport modes (WALKING, BIKE, TAXI, UBER)
        List<TransportMode> computedModes = transportModeRepository
                .findByEdgeResolutionAndIsActiveTrue(EnumEdgeResolution.COMPUTED);

        for (TransportMode mode : computedModes) {
            if (!context.isModeAllowed(mode.getCode())) {
                continue;
            }

            // Try to find a matching service area for this mode + location
            TransportServiceArea matchedArea = findMatchingServiceArea(mode, origin, destination);

            // Determine max distance: service area override > global default
            int maxDistanceM = resolveMaxDistance(mode, matchedArea);

            if (maxDistanceM > 0 && distanceM > maxDistanceM) {
                log.debug("Distance {}m exceeds max {}m for mode {} (area: {})",
                        (int) distanceM, maxDistanceM, mode.getCode(),
                        matchedArea != null ? matchedArea.getName() : "GLOBAL");
                continue;
            }

            // Calculate duration based on configured speed
            int speedKmh = mode.getDefaultSpeedKmh() != null ? mode.getDefaultSpeedKmh() : 5;
            int durationMin = (int) Math.ceil((distanceM / 1000.0) / speedKmh * 60);

            // Calculate cost from service area pricing (if available)
            int costCents = calculateCost(matchedArea, distanceM);

            // Resolve provider code from service area
            String providerCode = matchedArea != null && matchedArea.getProvider() != null
                    ? matchedArea.getProvider().getCode() : null;

            edges.add(new ResolvedEdge(
                    null,           // no edge ID (ephemeral)
                    origin,
                    destination,
                    mode.getCode(),
                    providerCode,
                    null,           // no service code
                    null,           // no departure time
                    null,           // no arrival time
                    durationMin,
                    (int) distanceM,
                    costCents,
                    0,              // zero emissions for now
                    "COMPUTED",
                    false,          // not persisted
                    matchedArea != null
                            ? Map.of("serviceAreaId", matchedArea.getId().toString(),
                                     "serviceAreaName", matchedArea.getName())
                            : Map.of()
            ));

            log.debug("ComputedEdge: {} → {} via {} = {}m, {}min, {}cents (area: {})",
                    origin.name(), destination.name(), mode.getCode(),
                    (int) distanceM, durationMin, costCents,
                    matchedArea != null ? matchedArea.getName() : "GLOBAL");
        }

        return edges;
    }

    /**
     * Find the best matching service area for the given mode and location.
     * Priority: RADIUS > CITY > COUNTRY > GLOBAL (most specific wins).
     */
    private TransportServiceArea findMatchingServiceArea(
            TransportMode mode, ResolvedLocation origin, ResolvedLocation destination) {

        List<TransportServiceArea> areas = serviceAreaRepository
                .findByTransportModeIdAndIsActiveTrue(mode.getId());

        TransportServiceArea bestMatch = null;
        int bestPriority = -1; // higher = better

        for (TransportServiceArea area : areas) {
            if (area.getAreaType() == null) continue;
            String areaType = area.getAreaType().getValue();

            switch (areaType) {
                case "RADIUS" -> {
                    if (bestPriority >= 4) continue; // already have RADIUS match
                    if (area.getCenterLat() != null && area.getCenterLon() != null && area.getRadiusM() != null) {
                        double centerLat = area.getCenterLat().doubleValue();
                        double centerLon = area.getCenterLon().doubleValue();
                        int radiusM = area.getRadiusM();
                        double distOrigin = haversine(origin.lat(), origin.lon(), centerLat, centerLon);
                        double distDest = haversine(destination.lat(), destination.lon(), centerLat, centerLon);
                        if (distOrigin <= radiusM || distDest <= radiusM) {
                            log.debug("ServiceArea RADIUS match: {} (origin={}m, dest={}m, radius={}m)",
                                    area.getName(), (int) distOrigin, (int) distDest, radiusM);
                            bestMatch = area;
                            bestPriority = 4;
                        }
                    }
                }
                case "CITY" -> {
                    if (bestPriority >= 3) continue;
                    if (area.getCity() != null) {
                        String city = area.getCity().toLowerCase();
                        boolean originMatch = origin.name() != null && origin.name().toLowerCase().contains(city);
                        boolean destMatch = destination.name() != null && destination.name().toLowerCase().contains(city);
                        if (originMatch || destMatch) {
                            log.debug("ServiceArea CITY match: {} (city={})", area.getName(), area.getCity());
                            bestMatch = area;
                            bestPriority = 3;
                        }
                    }
                }
                case "COUNTRY" -> {
                    if (bestPriority >= 2) continue;
                    if (area.getCountryIsoCode() != null) {
                        String areaCountry = area.getCountryIsoCode().toUpperCase();
                        boolean originMatch = areaCountry.equals(origin.countryIsoCode());
                        boolean destMatch = areaCountry.equals(destination.countryIsoCode());
                        if (originMatch || destMatch) {
                            log.debug("ServiceArea COUNTRY match: {} (country={})", area.getName(), areaCountry);
                            bestMatch = area;
                            bestPriority = 2;
                        }
                    }
                }
                case "GLOBAL" -> {
                    if (bestPriority >= 1) continue;
                    log.debug("ServiceArea GLOBAL match: {}", area.getName());
                    bestMatch = area;
                    bestPriority = 1;
                }
            }
        }

        return bestMatch;
    }

    /**
     * Resolve max distance: service area config > global mode default.
     */
    private int resolveMaxDistance(TransportMode mode, TransportServiceArea area) {
        if (area != null && area.getConfigJson() != null) {
            try {
                JsonNode config = mapper.readTree(area.getConfigJson());
                if (config.has("max_distance_m")) {
                    return config.get("max_distance_m").asInt();
                }
            } catch (Exception e) {
                log.warn("Failed to parse config_json for service area {}: {}", area.getName(), e.getMessage());
            }
        }
        // Global fallback
        return mode.getMaxWalkingAccessM() != null ? mode.getMaxWalkingAccessM() : Integer.MAX_VALUE;
    }

    /**
     * Calculate cost from service area pricing config.
     * Formula: base_fare + (distance_km * per_km_rate), min = min_fare
     */
    private int calculateCost(TransportServiceArea area, double distanceM) {
        if (area == null || area.getConfigJson() == null) {
            return 0; // No pricing info
        }

        try {
            JsonNode config = mapper.readTree(area.getConfigJson());
            JsonNode pricing = config.get("pricing");
            if (pricing == null) return 0;

            int baseFare = pricing.has("base_fare_cents") ? pricing.get("base_fare_cents").asInt() : 0;
            int perKm = pricing.has("per_km_cents") ? pricing.get("per_km_cents").asInt() : 0;
            int minFare = pricing.has("min_fare_cents") ? pricing.get("min_fare_cents").asInt() : 0;

            int cost = baseFare + (int) (distanceM / 1000.0 * perKm);
            return Math.max(cost, minFare);
        } catch (Exception e) {
            log.warn("Failed to calculate cost from service area {}: {}", area.getName(), e.getMessage());
            return 0;
        }
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
