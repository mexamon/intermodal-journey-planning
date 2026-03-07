package com.thy.cloud.service.api.resolver.location;

import com.thy.cloud.service.api.resolver.model.ResolvedLocation;

import java.util.List;
import java.util.Optional;

/**
 * Resolves user input (text query or coordinates) to Location nodes.
 * <p>
 * Implementations:
 * <ul>
 *     <li>{@code DbLocationResolver} — searches the {@code location} table via trigram index</li>
 *     <li>{@code GooglePlacesResolver} — geocodes via Google Places API (fallback)</li>
 * </ul>
 */
public interface LocationResolver {

    /**
     * Resolve a text query to matching locations.
     *
     * @param query user input (e.g. "Kadıköy", "SAW", "London Heathrow")
     * @param limit maximum results to return
     * @return list of resolved locations, ordered by relevance
     */
    List<ResolvedLocation> resolve(String query, int limit);

    /**
     * Resolve coordinates to the nearest known location.
     *
     * @param lat latitude
     * @param lon longitude
     * @return the nearest location, if within reasonable distance
     */
    Optional<ResolvedLocation> resolveByCoordinates(double lat, double lon);

    /**
     * The source type this resolver provides.
     */
    String getSource(); // "DB" or "GOOGLE_PLACES"
}
