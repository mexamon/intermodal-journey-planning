package com.thy.cloud.service.api.datasync.amadeus;

import com.thy.cloud.service.api.datasync.DataSourceSyncService;
import com.thy.cloud.service.api.datasync.SyncRequest;
import com.thy.cloud.service.api.datasync.SyncResult;
import com.thy.cloud.service.api.datasync.amadeus.model.AmadeusFlightOffer;
import com.thy.cloud.service.api.datasync.amadeus.model.AmadeusFlightOffer.Segment;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.entity.inventory.Provider;
import com.thy.cloud.service.dao.entity.transport.EdgeTrip;
import com.thy.cloud.service.dao.entity.transport.TransportMode;
import com.thy.cloud.service.dao.entity.transport.TransportationEdge;
import com.thy.cloud.service.dao.enums.EnumEdgeSource;
import com.thy.cloud.service.dao.enums.EnumEdgeStatus;
import com.thy.cloud.service.dao.enums.EnumScheduleType;
import com.thy.cloud.service.dao.repository.inventory.LocationRepository;
import com.thy.cloud.service.dao.repository.inventory.ProviderRepository;
import com.thy.cloud.service.dao.repository.transport.EdgeTripRepository;
import com.thy.cloud.service.dao.repository.transport.TransportModeRepository;
import com.thy.cloud.service.dao.repository.transport.TransportationEdgeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Syncs Amadeus flight data into the transportation_edge + edge_trip tables.
 * <p>
 * For each Amadeus flight offer, it creates:
 * <ol>
 *     <li>A {@code transportation_edge} (origin→destination route)</li>
 *     <li>An {@code edge_trip} (specific departure/arrival time)</li>
 * </ol>
 * <p>
 * Routes are de-duplicated: if the same origin→destination→carrier already exists,
 * only new trips are added. Existing edges are reused.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AmadeusSyncService implements DataSourceSyncService {

    private final AmadeusClient amadeusClient;
    private final LocationRepository locationRepository;
    private final ProviderRepository providerRepository;
    private final TransportModeRepository transportModeRepository;
    private final TransportationEdgeRepository edgeRepository;
    private final EdgeTripRepository edgeTripRepository;

    @Override
    public String getSourceType() {
        return "AMADEUS";
    }

    @Override
    @Transactional
    public SyncResult sync(SyncRequest request) {
        log.info("Starting Amadeus sync: {}", request);

        var resultBuilder = SyncResult.builder("AMADEUS");
        List<AmadeusFlightOffer> offers = amadeusClient.getAllScheduledFlights();

        if (offers.isEmpty()) {
            log.warn("No flight offers returned from Amadeus client");
            return resultBuilder.build();
        }

        // Pre-load references
        Optional<TransportMode> flightMode = transportModeRepository.findByCode("FLIGHT");
        if (flightMode.isEmpty()) {
            log.error("Transport mode FLIGHT not found in DB");
            return resultBuilder.errors(1).warn("Transport mode FLIGHT not found").build();
        }

        // Cache for de-duplication: "SAW→LHR:TK" → edge
        Map<String, TransportationEdge> edgeCache = new HashMap<>();
        // Cache for existing trips to avoid duplicates
        Set<String> existingTripKeys = new HashSet<>();

        int edgesCreated = 0, edgesUpdated = 0, tripsCreated = 0, errors = 0;

        for (AmadeusFlightOffer offer : offers) {
            try {
                for (var itinerary : offer.itineraries()) {
                    for (Segment seg : itinerary.segments()) {
                        // Resolve locations
                        Optional<Location> originOpt = locationRepository.findByIataCode(seg.departure().iataCode());
                        Optional<Location> destOpt = locationRepository.findByIataCode(seg.arrival().iataCode());

                        if (originOpt.isEmpty() || destOpt.isEmpty()) {
                            log.debug("Skipping flight {}{}: location not found ({} or {})",
                                    seg.carrierCode(), seg.number(),
                                    seg.departure().iataCode(), seg.arrival().iataCode());
                            continue;
                        }

                        // Resolve provider (airline)
                        Optional<Provider> providerOpt = providerRepository.findByCode(seg.carrierCode());

                        // De-duplication key for edge: origin→destination with carrier
                        String edgeKey = seg.departure().iataCode() + "→" + seg.arrival().iataCode()
                                + ":" + seg.carrierCode();

                        // Get or create edge
                        TransportationEdge edge = edgeCache.get(edgeKey);
                        if (edge == null) {
                            // Check if already in DB
                            List<TransportationEdge> existing = edgeRepository
                                    .findByOriginLocationIdAndDestinationLocationId(
                                            originOpt.get().getId(), destOpt.get().getId());

                            edge = existing.stream()
                                    .filter(e -> e.getSource() == EnumEdgeSource.AMADEUS)
                                    .filter(e -> providerOpt.map(p -> p.getId().equals(
                                            e.getProvider() != null ? e.getProvider().getId() : null)).orElse(true))
                                    .findFirst()
                                    .orElse(null);

                            if (edge == null) {
                                // Create new edge
                                edge = new TransportationEdge();
                                edge.setId(UUID.randomUUID());
                                edge.setOriginLocation(originOpt.get());
                                edge.setDestinationLocation(destOpt.get());
                                edge.setTransportMode(flightMode.get());
                                providerOpt.ifPresent(edge::setProvider);
                                edge.setScheduleType(EnumScheduleType.FIXED);
                                edge.setOperatingDaysMask((short) 127);
                                edge.setStatus(EnumEdgeStatus.ACTIVE);
                                edge.setSource(EnumEdgeSource.AMADEUS);
                                edge.setEstimatedDurationMin(parseDuration(seg.duration()));
                                edge.setDistanceM(estimateDistance(
                                        originOpt.get(), destOpt.get()));
                                edge.setCo2Grams(estimateCo2(edge.getDistanceM()));

                                edgeRepository.save(edge);
                                edgesCreated++;
                            } else {
                                edgesUpdated++;
                            }

                            edgeCache.put(edgeKey, edge);
                        }

                        // Create trip (if not duplicate)
                        String tripKey = edgeKey + ":"
                                + seg.departure().at() + "→" + seg.arrival().at()
                                + ":" + seg.number();

                        if (!existingTripKeys.contains(tripKey)) {
                            // Check DB for existing trip
                            List<EdgeTrip> existingTrips = edgeTripRepository.findByServiceCode(
                                    seg.carrierCode() + seg.number());

                            if (existingTrips.isEmpty()) {
                                EdgeTrip trip = new EdgeTrip();
                                trip.setId(UUID.randomUUID());
                                trip.setEdge(edge);
                                trip.setServiceCode(seg.carrierCode() + seg.number());
                                trip.setDepartureTime(seg.departure().at());
                                trip.setArrivalTime(seg.arrival().at());

                                // Convert operating days list to bitmask
                                short daysMask = operatingDaysToBitmask(seg.operatingDays());
                                trip.setOperatingDaysMask(daysMask);

                                // Cost from offer price
                                trip.setEstimatedCostCents(offer.price().totalCents());

                                edgeTripRepository.save(trip);
                                tripsCreated++;
                            }

                            existingTripKeys.add(tripKey);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Error processing Amadeus offer {}: {}", offer.id(), e.getMessage(), e);
                errors++;
            }
        }

        log.info("Amadeus sync complete: {} edges created, {} updated, {} trips created, {} errors",
                edgesCreated, edgesUpdated, tripsCreated, errors);

        return resultBuilder
                .edgesCreated(edgesCreated)
                .edgesUpdated(edgesUpdated)
                .tripsCreated(tripsCreated)
                .errors(errors)
                .build();
    }

    /**
     * Convert ISO-8601 duration string to minutes.
     * Example: "PT3H50M" → 230
     */
    private int parseDuration(String isoDuration) {
        if (isoDuration == null) return 0;
        try {
            java.time.Duration d = java.time.Duration.parse(isoDuration);
            return (int) d.toMinutes();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Estimate great-circle distance between two locations in meters.
     */
    private int estimateDistance(Location origin, Location dest) {
        if (origin.getLat() == null || dest.getLat() == null) return 0;
        double lat1 = origin.getLat().doubleValue(), lon1 = origin.getLon().doubleValue();
        double lat2 = dest.getLat().doubleValue(), lon2 = dest.getLon().doubleValue();
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (int) (6_371_000 * c);
    }

    /**
     * Estimate CO₂ emissions in grams (~90g per passenger-km for flights).
     */
    private int estimateCo2(Integer distanceM) {
        if (distanceM == null || distanceM == 0) return 0;
        return (int) (distanceM / 1000.0 * 90);
    }

    /**
     * Convert list of day numbers [1,2,3,4,5,6,7] to 7-bit bitmask.
     * 1=Monday(bit0), 7=Sunday(bit6).
     */
    private short operatingDaysToBitmask(List<Integer> days) {
        if (days == null || days.isEmpty()) return 127; // all days
        short mask = 0;
        for (int day : days) {
            mask |= (short) (1 << (day - 1));
        }
        return mask;
    }
}
