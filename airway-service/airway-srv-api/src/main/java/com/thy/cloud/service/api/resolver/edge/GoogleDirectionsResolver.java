package com.thy.cloud.service.api.resolver.edge;

import com.thy.cloud.service.api.config.GoogleApiConfig;
import com.thy.cloud.service.api.resolver.model.EdgeSearchContext;
import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Resolves edges via Google Directions API for taxi/driving/transit routes.
 * <p>
 * Used for first-mile and last-mile segments: user location → nearest hub.
 * Results are ephemeral (not stored in DB), should be cached in Redis (TTL ~15min).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoogleDirectionsResolver implements EdgeResolver {

    private final GoogleApiConfig googleApiConfig;
    private final RestTemplate googleRestTemplate;

    private static final String DIRECTIONS_URL =
            "https://maps.googleapis.com/maps/api/directions/json";

    @Override
    public String getResolution() {
        return "API_DYNAMIC";
    }

    @Override
    public List<ResolvedEdge> resolve(ResolvedLocation origin,
                                       ResolvedLocation destination,
                                       EdgeSearchContext context) {
        if (!googleApiConfig.isDirectionsEnabled()) {
            log.debug("Google Directions API disabled");
            return List.of();
        }

        if (origin == null || destination == null) {
            return List.of();
        }

        List<ResolvedEdge> edges = new ArrayList<>();

        // Resolve driving (taxi/Uber)
        if (context.isModeAllowed("UBER") || context.isModeAllowed("TAXI") || context.allowedModes().isEmpty()) {
            resolveMode(origin, destination, "driving", "UBER").ifPresent(edges::add);
        }

        // Resolve walking (as enhancement to COMPUTED — more accurate with real roads)
        if (context.isModeAllowed("WALKING") || context.allowedModes().isEmpty()) {
            resolveMode(origin, destination, "walking", "WALKING").ifPresent(edges::add);
        }

        return edges;
    }

    @SuppressWarnings("unchecked")
    private Optional<ResolvedEdge> resolveMode(ResolvedLocation origin, ResolvedLocation destination,
                                                String googleMode, String ourModeCode) {
        try {
            String url = String.format(
                    "%s?origin=%f,%f&destination=%f,%f&mode=%s&key=%s",
                    DIRECTIONS_URL,
                    origin.lat(), origin.lon(),
                    destination.lat(), destination.lon(),
                    googleMode,
                    googleApiConfig.getApiKey()
            );

            Map<String, Object> response = googleRestTemplate.getForObject(url, Map.class);

            if (response == null || !"OK".equals(response.get("status"))) {
                log.warn("Google Directions API returned status: {}",
                        response != null ? response.get("status") : "null");
                return Optional.empty();
            }

            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
            if (routes == null || routes.isEmpty()) return Optional.empty();

            Map<String, Object> route = routes.get(0);
            List<Map<String, Object>> legs = (List<Map<String, Object>>) route.get("legs");
            if (legs == null || legs.isEmpty()) return Optional.empty();

            Map<String, Object> leg = legs.get(0);
            Map<String, Object> duration = (Map<String, Object>) leg.get("duration");
            Map<String, Object> distance = (Map<String, Object>) leg.get("distance");

            int durationSec = ((Number) duration.get("value")).intValue();
            int distanceM = ((Number) distance.get("value")).intValue();
            int durationMin = (int) Math.ceil(durationSec / 60.0);

            // Estimate cost for driving (rough formula: base + per-km)
            Integer costCents = null;
            if ("driving".equals(googleMode)) {
                // ~₺30 base + ₺15/km (İstanbul taxi estimate)
                costCents = 3000 + (int) (distanceM / 1000.0 * 1500);
            }

            log.debug("GoogleDirections: {} → {} via {} = {}m, {}min",
                    origin.name(), destination.name(), ourModeCode, distanceM, durationMin);

            return Optional.of(ResolvedEdge.ephemeral(
                    origin, destination,
                    ourModeCode,
                    "GOOGLE_API",
                    durationMin,
                    distanceM,
                    costCents,
                    "driving".equals(googleMode) ? "TRY" : null,
                    "driving".equals(googleMode)
                            ? (int) (distanceM * 0.12) // ~120g CO₂ per km for cars
                            : 0
            ));

        } catch (Exception e) {
            log.error("Google Directions API error: {} → {} ({}): {}",
                    origin.name(), destination.name(), googleMode, e.getMessage());
            return Optional.empty();
        }
    }
}
