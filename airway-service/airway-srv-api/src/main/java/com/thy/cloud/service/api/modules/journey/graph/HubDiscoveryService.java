package com.thy.cloud.service.api.modules.journey.graph;

import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.enums.EnumLocationType;
import com.thy.cloud.service.dao.repository.inventory.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Discovers nearby hub locations (airports, stations) using bounding-box KNN search.
 * Used in Phase A/C to find first-mile and last-mile connection points.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HubDiscoveryService {

    private static final double HUB_SEARCH_RADIUS_M = 50_000; // 50km

    private final LocationRepository locationRepository;

    /**
     * Discover hubs near a location. If the location itself is a hub, it's included.
     */
    public List<ResolvedLocation> discoverHubs(ResolvedLocation location) {
        List<ResolvedLocation> hubs = new ArrayList<>();

        if ("AIRPORT".equals(location.type()) || "STATION".equals(location.type())) {
            hubs.add(location);
            if (location.persisted()) {
                findNearbyPersistedLocations(location, "AIRPORT", 7).forEach(hub -> {
                    if (!locKey(hub).equals(locKey(location))) hubs.add(hub);
                });
            }
            return hubs;
        }

        hubs.addAll(findNearbyPersistedLocations(location, "AIRPORT", 7));
        hubs.addAll(findNearbyPersistedLocations(location, "STATION", 5));

        if (hubs.isEmpty()) {
            log.warn("No hubs found near {} ({}, {})", location.name(), location.lat(), location.lon());
        }

        return hubs;
    }

    /**
     * Find persisted locations of a given type within bounding-box radius, sorted by distance.
     */
    public List<ResolvedLocation> findNearbyPersistedLocations(ResolvedLocation origin, String type, int limit) {
        double radiusDeg = HUB_SEARCH_RADIUS_M / 111_000.0;
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

    // ── Shared helpers ──

    public static String locKey(ResolvedLocation loc) {
        if (loc.id() != null) return loc.id().toString();
        return String.format("%.4f,%.4f", loc.lat(), loc.lon());
    }

    public static ResolvedLocation toResolvedLocation(Location loc) {
        return ResolvedLocation.fromDb(
                loc.getId(), loc.getName(), loc.getIataCode(),
                loc.getLat() != null ? loc.getLat().doubleValue() : 0,
                loc.getLon() != null ? loc.getLon().doubleValue() : 0,
                loc.getType() != null ? loc.getType().getValue() : "AIRPORT",
                loc.getCountryIsoCode(),
                loc.getTimezone()
        );
    }

    public static double haversineM(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6_371_000 * c;
    }
}
