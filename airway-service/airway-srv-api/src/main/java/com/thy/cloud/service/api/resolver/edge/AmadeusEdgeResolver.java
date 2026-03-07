package com.thy.cloud.service.api.resolver.edge;

import com.thy.cloud.service.api.datasync.amadeus.AmadeusClient;
import com.thy.cloud.service.api.datasync.amadeus.model.AmadeusFlightOffer;
import com.thy.cloud.service.api.datasync.amadeus.model.AmadeusFlightOffer.Segment;
import com.thy.cloud.service.api.resolver.model.EdgeSearchContext;
import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/**
 * Live Amadeus edge resolver — queries MockAmadeusClient at search time.
 * No DB writes. Data stays in memory, queried on every search.
 * <p>
 * When real Amadeus API is connected, this resolver will call the real API
 * instead of the mock — zero changes needed in search orchestration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AmadeusEdgeResolver implements EdgeResolver {

    private final AmadeusClient amadeusClient;

    @Override
    public String getResolution() {
        return "AMADEUS_LIVE";
    }

    @Override
    public List<ResolvedEdge> resolve(ResolvedLocation origin,
                                       ResolvedLocation destination,
                                       EdgeSearchContext context) {
        if (origin == null || origin.iataCode() == null || origin.iataCode().isBlank()) {
            return List.of();
        }

        if (!context.isModeAllowed("FLIGHT")) {
            return List.of();
        }

        // Query Amadeus for flights from this origin
        String destIata = destination != null ? destination.iataCode() : null;

        List<AmadeusFlightOffer> offers;
        if (destIata != null && !destIata.isBlank()) {
            // Specific route search
            offers = amadeusClient.searchFlights(origin.iataCode(), destIata,
                    context.travelDate(), 20);
        } else {
            // All flights from origin — filter later
            offers = amadeusClient.getAllScheduledFlights().stream()
                    .filter(f -> {
                        var seg = f.itineraries().get(0).segments().get(0);
                        return seg.departure().iataCode().equals(origin.iataCode());
                    })
                    .toList();
        }

        if (offers.isEmpty()) return List.of();

        // Filter by operating day
        int dayBit = context.travelDate() != null
                ? dayOfWeekBit(context.travelDate().getDayOfWeek()) : 127;

        List<ResolvedEdge> edges = new ArrayList<>();
        for (AmadeusFlightOffer offer : offers) {
            for (var itinerary : offer.itineraries()) {
                for (Segment seg : itinerary.segments()) {
                    // Check operating days
                    if (seg.operatingDays() != null && !seg.operatingDays().isEmpty()) {
                        short mask = operatingDaysToBitmask(seg.operatingDays());
                        if ((mask & dayBit) == 0) continue;
                    }

                    // Build origin/destination ResolvedLocations for this segment
                    ResolvedLocation segOrigin = ResolvedLocation.virtual(
                            seg.departure().iataCode() + " Airport",
                            0, 0, "AIRPORT"
                    );
                    ResolvedLocation segDest = ResolvedLocation.virtual(
                            seg.arrival().iataCode() + " Airport",
                            0, 0, "AIRPORT"
                    );

                    // If we have the actual resolved locations, use those
                    if (origin.iataCode() != null && origin.iataCode().equals(seg.departure().iataCode())) {
                        segOrigin = origin;
                    }
                    if (destination != null && destination.iataCode() != null
                            && destination.iataCode().equals(seg.arrival().iataCode())) {
                        segDest = destination;
                    }

                    int durationMin = parseDuration(seg.duration());

                    edges.add(ResolvedEdge.ephemeral(
                            segOrigin, segDest,
                            "FLIGHT",
                            "AMADEUS",
                            durationMin,
                            0,  // distance calculated from coords if available
                            offer.price().totalCents(),
                            estimateCo2(durationMin)
                    ));

                    // Override departure/arrival times
                    ResolvedEdge last = edges.get(edges.size() - 1);
                    edges.set(edges.size() - 1, new ResolvedEdge(
                            last.id(), segOrigin, segDest,
                            "FLIGHT",
                            seg.carrierCode(),
                            seg.carrierCode() + seg.number(),
                            seg.departure().at(),
                            seg.arrival().at(),
                            durationMin,
                            last.distanceM(),
                            offer.price().totalCents(),
                            estimateCo2(durationMin),
                            "AMADEUS",
                            false,
                            Map.of()
                    ));
                }
            }
        }

        log.debug("AmadeusEdgeResolver: {} flights from {} (dest={})",
                edges.size(), origin.iataCode(), destIata);
        return edges;
    }

    private int parseDuration(String isoDuration) {
        if (isoDuration == null) return 0;
        try {
            return (int) java.time.Duration.parse(isoDuration).toMinutes();
        } catch (Exception e) { return 0; }
    }

    private int estimateCo2(int durationMin) {
        // ~150kg CO₂ per hour of flight
        return (int) (durationMin / 60.0 * 150_000);
    }

    private int dayOfWeekBit(DayOfWeek dow) {
        return 1 << (dow.getValue() - 1);
    }

    private short operatingDaysToBitmask(List<Integer> days) {
        short mask = 0;
        for (int day : days) mask |= (short) (1 << (day - 1));
        return mask;
    }
}
