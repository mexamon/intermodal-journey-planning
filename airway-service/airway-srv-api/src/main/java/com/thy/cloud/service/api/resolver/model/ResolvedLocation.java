package com.thy.cloud.service.api.resolver.model;

import java.util.UUID;

/**
 * A resolved location — may be from DB (persisted) or from Google Places API (virtual).
 */
public record ResolvedLocation(
        UUID id,              // null if virtual (Google-only)
        String name,
        String iataCode,      // null for non-airports
        double lat,
        double lon,
        String type,          // AIRPORT, CITY, STATION, POI
        String source,        // DB, GOOGLE_PLACES
        boolean persisted,    // true = in DB, false = virtual
        String countryIsoCode // ISO 3166-1 alpha-2, e.g. "TR", "DE"
) {
    public static ResolvedLocation fromDb(UUID id, String name, String iataCode,
                                          double lat, double lon, String type,
                                          String countryIsoCode) {
        return new ResolvedLocation(id, name, iataCode, lat, lon, type, "DB", true, countryIsoCode);
    }

    public static ResolvedLocation virtual(String name, double lat, double lon, String type) {
        return new ResolvedLocation(null, name, null, lat, lon, type, "GOOGLE_PLACES", false, null);
    }
}
