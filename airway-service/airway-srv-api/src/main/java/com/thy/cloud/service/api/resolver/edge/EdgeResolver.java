package com.thy.cloud.service.api.resolver.edge;

import com.thy.cloud.service.api.resolver.model.EdgeSearchContext;
import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import com.thy.cloud.service.api.resolver.model.ResolvedLocation;

import java.util.List;

/**
 * Resolves transportation edges between locations.
 * <p>
 * Implementations:
 * <ul>
 *     <li>{@code StaticEdgeResolver} — queries persisted edges from DB (MANUAL, AMADEUS, GTFS)</li>
 *     <li>{@code ComputedEdgeResolver} — calculates walking/biking edges via Haversine</li>
 *     <li>{@code GoogleDirectionsResolver} — calls Google Directions API for taxi/transit</li>
 * </ul>
 */
public interface EdgeResolver {

    /**
     * The edge resolution strategy this resolver handles.
     *
     * @return "STATIC", "COMPUTED", or "API_DYNAMIC"
     */
    String getResolution();

    /**
     * Resolve edges from an origin, optionally directed toward a destination.
     *
     * @param origin      the starting location
     * @param destination optional destination hint (may be null for BFS exploration)
     * @param context     search parameters (date, allowed modes, etc.)
     * @return list of resolved edges from the origin
     */
    List<ResolvedEdge> resolve(ResolvedLocation origin,
                                ResolvedLocation destination,
                                EdgeSearchContext context);
}
