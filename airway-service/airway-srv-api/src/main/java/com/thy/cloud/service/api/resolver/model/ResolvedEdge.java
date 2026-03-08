package com.thy.cloud.service.api.resolver.model;

import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

/**
 * A resolved transportation edge — may be from DB (STATIC), API (DYNAMIC), or calculation (COMPUTED).
 * This is the unified output that all EdgeResolver implementations produce.
 */
public record ResolvedEdge(
        UUID id,                      // null if ephemeral
        ResolvedLocation origin,
        ResolvedLocation destination,
        String transportModeCode,     // FLIGHT, WALKING, UBER...
        String providerCode,          // TK, PC, IETT, null
        String serviceCode,           // TK1987, M1, null
        LocalTime departureTime,
        LocalTime arrivalTime,
        int durationMin,
        int distanceM,
        Integer costCents,            // null for walking
        String currency,              // ISO currency code: TRY, EUR, USD
        Integer co2Grams,
        String source,                // MANUAL, AMADEUS, GTFS, GOOGLE_API, COMPUTED
        boolean persisted,            // true = from DB, false = ephemeral
        Map<String, Object> attrs     // cabin, aircraft, platform, etc.
) {
    /**
     * Create an ephemeral edge (COMPUTED or API_DYNAMIC — not stored in DB).
     */
    public static ResolvedEdge ephemeral(
            ResolvedLocation origin, ResolvedLocation destination,
            String modeCode, String source,
            int durationMin, int distanceM, Integer costCents, String currency, Integer co2Grams) {
        return new ResolvedEdge(
                null, origin, destination, modeCode,
                null, null, null, null,
                durationMin, distanceM, costCents, currency, co2Grams,
                source, false, Map.of()
        );
    }
}
