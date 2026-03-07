package com.thy.cloud.service.api.resolver.location;

import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.repository.inventory.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Resolves locations from the database using name search and IATA code lookup.
 * This is the primary (free, fast) resolver — should always be tried first.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DbLocationResolver implements LocationResolver {

    private final LocationRepository locationRepository;

    @Override
    public String getSource() {
        return "DB";
    }

    @Override
    public List<ResolvedLocation> resolve(String query, int limit) {
        if (query == null || query.length() < 2) {
            return List.of();
        }

        // 1. Try exact IATA code match first (fast, unique index)
        if (query.length() == 3 && query.equals(query.toUpperCase())) {
            Optional<Location> byIata = locationRepository.findByIataCode(query);
            if (byIata.isPresent()) {
                return List.of(toResolved(byIata.get()));
            }
        }

        // 2. Fall back to searchable locations with name matching
        // Uses trigram index via native query for fuzzy search
        var page = locationRepository.findByIsSearchableTrueOrderBySearchPriorityDesc(
                PageRequest.of(0, limit * 3) // fetch extra, filter client-side
        );

        String q = query.toLowerCase();
        return page.getContent().stream()
                .filter(loc -> {
                    String name = loc.getName().toLowerCase();
                    String city = loc.getCity() != null ? loc.getCity().toLowerCase() : "";
                    String iata = loc.getIataCode() != null ? loc.getIataCode().toLowerCase() : "";
                    return name.contains(q) || city.contains(q) || iata.contains(q);
                })
                .limit(limit)
                .map(this::toResolved)
                .toList();
    }

    @Override
    public Optional<ResolvedLocation> resolveByCoordinates(double lat, double lon) {
        // PostGIS-based nearest location query — future enhancement
        // For now, return empty
        log.debug("Coordinate-based location resolution not yet implemented: ({}, {})", lat, lon);
        return Optional.empty();
    }

    private ResolvedLocation toResolved(Location loc) {
        String typeValue = loc.getType() != null ? loc.getType().getValue() : "AIRPORT";
        return ResolvedLocation.fromDb(
                loc.getId(),
                loc.getName(),
                loc.getIataCode(),
                loc.getLat() != null ? loc.getLat().doubleValue() : 0,
                loc.getLon() != null ? loc.getLon().doubleValue() : 0,
                typeValue
        );
    }
}
