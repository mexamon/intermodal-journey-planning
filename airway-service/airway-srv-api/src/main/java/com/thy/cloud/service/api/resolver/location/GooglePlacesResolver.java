package com.thy.cloud.service.api.resolver.location;

import com.thy.cloud.service.api.config.GoogleApiConfig;
import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Resolves locations via Google Places API (New).
 * Used as a fallback when DbLocationResolver returns no results.
 *
 * <p>Calls Google Places Text Search to geocode user input like
 * "Kadıköy" or "Taksim Meydanı" into lat/lon coordinates.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GooglePlacesResolver implements LocationResolver {

    private final GoogleApiConfig googleApiConfig;
    private final RestTemplate googleRestTemplate;

    private static final String PLACES_TEXT_SEARCH_URL =
            "https://places.googleapis.com/v1/places:searchText";

    @Override
    public String getSource() {
        return "GOOGLE_PLACES";
    }

    @Override
    public List<ResolvedLocation> resolve(String query, int limit) {
        if (!googleApiConfig.isPlacesEnabled()) {
            log.debug("Google Places API disabled");
            return List.of();
        }

        try {
            // Use Google Places Text Search (New)
            var headers = new org.springframework.http.HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("X-Goog-Api-Key", googleApiConfig.getApiKey());
            headers.set("X-Goog-FieldMask",
                    "places.displayName,places.formattedAddress,places.location,places.types,places.id");

            var body = Map.of(
                    "textQuery", query,
                    "maxResultCount", Math.min(limit, 10)
            );

            var request = new org.springframework.http.HttpEntity<>(body, headers);
            var response = googleRestTemplate.postForObject(PLACES_TEXT_SEARCH_URL, request, Map.class);

            if (response == null || !response.containsKey("places")) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> places = (List<Map<String, Object>>) response.get("places");

            return places.stream()
                    .map(this::toResolved)
                    .filter(Objects::nonNull)
                    .limit(limit)
                    .toList();

        } catch (Exception e) {
            log.error("Google Places API error for query '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    @Override
    public Optional<ResolvedLocation> resolveByCoordinates(double lat, double lon) {
        if (!googleApiConfig.isPlacesEnabled()) {
            return Optional.empty();
        }
        // Reverse geocoding — future enhancement
        log.debug("Google reverse geocoding not yet implemented");
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private ResolvedLocation toResolved(Map<String, Object> place) {
        try {
            Map<String, Object> location = (Map<String, Object>) place.get("location");
            Map<String, Object> displayName = (Map<String, Object>) place.get("displayName");

            double lat = ((Number) location.get("latitude")).doubleValue();
            double lon = ((Number) location.get("longitude")).doubleValue();
            String name = (String) displayName.get("text");

            // Determine type from Google place types
            List<String> types = (List<String>) place.getOrDefault("types", List.of());
            String type = "POI";
            if (types.contains("airport")) type = "AIRPORT";
            else if (types.contains("train_station") || types.contains("transit_station")) type = "STATION";
            else if (types.contains("locality") || types.contains("administrative_area_level_1")) type = "CITY";

            return ResolvedLocation.virtual(name, lat, lon, type);
        } catch (Exception e) {
            log.warn("Could not parse Google place: {}", e.getMessage());
            return null;
        }
    }
}
