package com.thy.cloud.service.api.resolver.edge;

import com.thy.cloud.service.api.resolver.model.EdgeSearchContext;
import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import com.thy.cloud.service.dao.entity.transport.TransportationEdge;
import com.thy.cloud.service.dao.entity.transport.EdgeTrip;
import com.thy.cloud.service.dao.enums.EnumEdgeStatus;
import com.thy.cloud.service.dao.repository.transport.TransportationEdgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * Resolves STATIC edges from the database.
 * Serves all persisted edges regardless of source (MANUAL, AMADEUS, GTFS).
 * <p>
 * This is the primary edge resolver for scheduled transport: flights, trains, buses, ferries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaticEdgeResolver implements EdgeResolver {

    private final TransportationEdgeRepository edgeRepository;

    @Override
    public String getResolution() {
        return "STATIC";
    }

    @Override
    public List<ResolvedEdge> resolve(ResolvedLocation origin,
                                       ResolvedLocation destination,
                                       EdgeSearchContext context) {
        if (origin == null || origin.id() == null) {
            log.debug("Cannot resolve static edges for non-persisted origin: {}", origin);
            return List.of();
        }

        // Fetch edges from DB (uses fetch join for eager loading)
        List<TransportationEdge> edges = edgeRepository.findByOriginLocationIdAndStatus(
                origin.id(), EnumEdgeStatus.ACTIVE
        );

        if (edges.isEmpty()) {
            return List.of();
        }

        LocalDate travelDate = context.travelDate();
        int dayBit = travelDate != null ? dayOfWeekBit(travelDate.getDayOfWeek()) : 127;

        List<ResolvedEdge> result = new ArrayList<>();

        for (TransportationEdge edge : edges) {
            // Filter by allowed modes
            String modeCode = edge.getTransportMode() != null
                    ? edge.getTransportMode().getCode() : "UNKNOWN";
            if (!context.isModeAllowed(modeCode)) {
                continue;
            }

            // Filter by destination if specified
            if (destination != null && destination.id() != null
                    && !edge.getDestinationLocation().getId().equals(destination.id())) {
                continue;
            }

            // Check validity window (edge-level, deprecated but still checked)
            if (travelDate != null) {
                if (edge.getValidFrom() != null && travelDate.isBefore(edge.getValidFrom())) continue;
                if (edge.getValidTo() != null && travelDate.isAfter(edge.getValidTo())) continue;
            }

            // Check operating days on the edge level
            if ((edge.getOperatingDaysMask() & dayBit) == 0) {
                continue;
            }

            // Resolve trips for this edge
            List<EdgeTrip> trips = edge.getTrips();
            if (trips != null && !trips.isEmpty()) {
                for (EdgeTrip trip : trips) {
                    // Check trip-level operating days and validity
                    if ((trip.getOperatingDaysMask() & dayBit) == 0) continue;
                    if (travelDate != null && trip.getValidFrom() != null
                            && travelDate.isBefore(trip.getValidFrom())) continue;
                    if (travelDate != null && trip.getValidTo() != null
                            && travelDate.isAfter(trip.getValidTo())) continue;

                    result.add(toResolvedEdge(edge, trip, modeCode));
                }
            } else {
                // No trips — use edge-level schedule (frequency-based or on-demand)
                result.add(toResolvedEdge(edge, null, modeCode));
            }
        }

        log.debug("StaticEdgeResolver: {} edges found from origin {}", result.size(), origin.name());
        return result;
    }

    private ResolvedEdge toResolvedEdge(TransportationEdge edge, EdgeTrip trip, String modeCode) {
        var origin = toResolvedLocation(edge.getOriginLocation());
        var dest = toResolvedLocation(edge.getDestinationLocation());

        String providerCode = edge.getProvider() != null ? edge.getProvider().getCode() : null;
        String serviceCode = trip != null ? trip.getServiceCode() : edge.getServiceCode();
        String source = edge.getSource() != null ? edge.getSource().getValue() : "MANUAL";

        return new ResolvedEdge(
                edge.getId(),
                origin,
                dest,
                modeCode,
                providerCode,
                serviceCode,
                trip != null ? trip.getDepartureTime() : edge.getDepartureTime(),
                trip != null ? trip.getArrivalTime() : edge.getArrivalTime(),
                edge.getEstimatedDurationMin() != null ? edge.getEstimatedDurationMin() : 0,
                edge.getDistanceM() != null ? edge.getDistanceM() : 0,
                trip != null && trip.getEstimatedCostCents() != null
                        ? trip.getEstimatedCostCents()
                        : edge.getEstimatedCostCents(),
                "EUR",  // TODO: source from fare table currency when available
                edge.getCo2Grams(),
                source,
                true,
                Map.of()
        );
    }

    private ResolvedLocation toResolvedLocation(com.thy.cloud.service.dao.entity.inventory.Location loc) {
        return ResolvedLocation.fromDb(
                loc.getId(),
                loc.getName(),
                loc.getIataCode(),
                loc.getLat() != null ? loc.getLat().doubleValue() : 0,
                loc.getLon() != null ? loc.getLon().doubleValue() : 0,
                loc.getType() != null ? loc.getType().getValue() : "AIRPORT",
                loc.getCountryIsoCode(),
                loc.getTimezone()
        );
    }

    private int dayOfWeekBit(DayOfWeek dow) {
        return 1 << (dow.getValue() - 1); // Mon=bit0, Tue=bit1, ..., Sun=bit6
    }
}
