package com.thy.cloud.service.api.resolver.edge;

import com.thy.cloud.service.api.datasync.gtfs.GtfsParser;
import com.thy.cloud.service.api.datasync.gtfs.GtfsParser.GtfsFeed;
import com.thy.cloud.service.api.datasync.gtfs.model.*;
import com.thy.cloud.service.api.resolver.model.EdgeSearchContext;
import com.thy.cloud.service.api.resolver.model.ResolvedEdge;
import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GTFS edge resolver — reads GTFS feeds from classpath at startup.
 * <p>
 * Feeds are stored under {@code classpath:gtfs/<city>/} with standard GTFS files:
 * agency.txt, stops.txt, routes.txt, trips.txt, stop_times.txt, calendar.txt
 * <p>
 * All feeds are merged into a single in-memory index and queried on every search.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GtfsEdgeResolver implements EdgeResolver {

    private final GtfsParser gtfsParser;

    // In-memory GTFS index (loaded once, queried on every search)
    private Map<String, GtfsStop> stopIndex;
    private Map<String, GtfsRoute> routeIndex;
    private Map<String, GtfsCalendar> calendarIndex;
    private Map<String, List<GtfsStopTime>> stopTimesByTrip;
    private List<GtfsTrip> trips;

    // Pre-built edge list: consecutive stop pairs with their schedules
    private List<GtfsEdgeEntry> edgeEntries;

    // Fare map by route_id (cents)
    private static final Map<String, Integer> FARE_BY_ROUTE = Map.ofEntries(
            // Istanbul
            Map.entry("M1", 750), Map.entry("M2", 750), Map.entry("MR", 750),
            // İzmir
            Map.entry("IZBAN", 1500), Map.entry("T2", 1000),
            // Frankfurt (EUR cents)
            Map.entry("S8", 520), Map.entry("S9", 520), Map.entry("U4", 310),
            // Ankara
            Map.entry("ANK_M1", 1000), Map.entry("ANK", 1000), Map.entry("ESB", 5500),
            // Berlin (EUR cents)
            Map.entry("BER_S9", 380), Map.entry("BER_S45", 380), Map.entry("U7", 310), Map.entry("U2", 310),
            // London (GBP pence)
            Map.entry("EL", 530), Map.entry("PICC", 530)
    );

    private static final Map<String, String> CURRENCY_BY_ROUTE = Map.ofEntries(
            Map.entry("M1", "TRY"), Map.entry("M2", "TRY"), Map.entry("MR", "TRY"),
            Map.entry("IZBAN", "TRY"), Map.entry("T2", "TRY"),
            Map.entry("S8", "EUR"), Map.entry("S9", "EUR"), Map.entry("U4", "EUR"),
            Map.entry("ANK_M1", "TRY"), Map.entry("ANK", "TRY"), Map.entry("ESB", "TRY"),
            Map.entry("BER_S9", "EUR"), Map.entry("BER_S45", "EUR"), Map.entry("U7", "EUR"), Map.entry("U2", "EUR"),
            Map.entry("EL", "GBP"), Map.entry("PICC", "GBP")
    );

    @PostConstruct
    void loadFeeds() {
        GtfsFeed mergedFeed = new GtfsFeed();

        // Scan classpath:gtfs/*/ directories
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Set<String> cityDirs = new LinkedHashSet<>();

        try {
            // Discover city directories
            Resource[] stopFiles = resolver.getResources("classpath:gtfs/*/stops.txt");
            for (Resource r : stopFiles) {
                String uri = r.getURI().toString();
                // Extract city name: .../gtfs/istanbul/stops.txt → istanbul
                String[] parts = uri.split("/gtfs/");
                if (parts.length > 1) {
                    String city = parts[1].split("/")[0];
                    cityDirs.add(city);
                }
            }
        } catch (IOException e) {
            log.warn("Failed to discover GTFS cities: {}", e.getMessage());
        }

        for (String city : cityDirs) {
            try {
                String prefix = "classpath:gtfs/" + city + "/";
                GtfsFeed feed = gtfsParser.parseFromCsvStrings(
                        readResource(resolver, prefix + "agency.txt"),
                        readResource(resolver, prefix + "stops.txt"),
                        readResource(resolver, prefix + "routes.txt"),
                        readResource(resolver, prefix + "trips.txt"),
                        readResource(resolver, prefix + "stop_times.txt"),
                        readResource(resolver, prefix + "calendar.txt")
                );
                // Merge into global feed
                mergedFeed.agencies.addAll(feed.agencies);
                mergedFeed.stops.addAll(feed.stops);
                mergedFeed.routes.addAll(feed.routes);
                mergedFeed.trips.addAll(feed.trips);
                mergedFeed.stopTimes.addAll(feed.stopTimes);
                mergedFeed.calendars.addAll(feed.calendars);

                log.info("Loaded GTFS feed [{}]: {} stops, {} routes, {} trips",
                        city, feed.stops.size(), feed.routes.size(), feed.trips.size());
            } catch (Exception e) {
                log.warn("Failed to load GTFS feed [{}]: {}", city, e.getMessage());
            }
        }

        // Build indices
        stopIndex = mergedFeed.stops.stream().collect(Collectors.toMap(GtfsStop::stopId, s -> s, (a, b) -> a));
        routeIndex = mergedFeed.routes.stream().collect(Collectors.toMap(GtfsRoute::routeId, r -> r, (a, b) -> a));
        calendarIndex = mergedFeed.calendars.stream().collect(Collectors.toMap(GtfsCalendar::serviceId, c -> c, (a, b) -> a));
        stopTimesByTrip = mergedFeed.stopTimes.stream().collect(Collectors.groupingBy(GtfsStopTime::tripId));
        trips = mergedFeed.trips;

        // Pre-build edge entries for fast querying
        edgeEntries = buildEdgeEntries();
        log.info("GtfsEdgeResolver: loaded {} cities, {} total stops, {} routes, {} edge entries",
                cityDirs.size(), stopIndex.size(), routeIndex.size(), edgeEntries.size());
    }

    private String readResource(PathMatchingResourcePatternResolver resolver, String location) {
        try {
            Resource resource = resolver.getResource(location);
            if (!resource.exists()) return null;
            try (InputStream is = resource.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getResolution() {
        return "GTFS_LIVE";
    }

    @Override
    public List<ResolvedEdge> resolve(ResolvedLocation origin,
                                       ResolvedLocation destination,
                                       EdgeSearchContext context) {
        if (origin == null) return List.of();

        log.info("GTFS resolve: origin={}({},{}) dest={}({},{}), totalEdgeEntries={}",
                origin.name(), origin.lat(), origin.lon(),
                destination != null ? destination.name() : "null",
                destination != null ? destination.lat() : 0,
                destination != null ? destination.lon() : 0,
                edgeEntries.size());

        // Find matching GTFS stops near origin
        List<String> originStopIds = findNearbyStopIds(origin, 10000); // 10km radius
        log.info("GTFS origin stops within 3km: {}", originStopIds);
        if (originStopIds.isEmpty()) return List.of();

        List<String> destStopIds = destination != null
                ? findNearbyStopIds(destination, 10000) : List.of();
        log.info("GTFS dest stops within 3km: {}", destStopIds);

        int dayBit = context.travelDate() != null
                ? dayOfWeekBit(context.travelDate().getDayOfWeek()) : 127;

        List<ResolvedEdge> results = new ArrayList<>();
        int skipOrigin = 0, skipDest = 0, skipMode = 0, skipDay = 0, skipTime = 0;

        for (GtfsEdgeEntry entry : edgeEntries) {
            if (!originStopIds.contains(entry.fromStopId)) { skipOrigin++; continue; }
            if (!destStopIds.isEmpty() && !destStopIds.contains(entry.toStopId)) { skipDest++; continue; }

            String modeCode = entry.modeCode;
            if (!context.isModeAllowed(modeCode)) { skipMode++; continue; }
            if ((entry.daysMask & dayBit) == 0) { skipDay++; continue; }
            if (context.preferredTime() != null
                    && entry.departureTime.isBefore(context.preferredTime())) { skipTime++; continue; }

            GtfsStop fromStop = stopIndex.get(entry.fromStopId);
            GtfsStop toStop = stopIndex.get(entry.toStopId);
            if (fromStop == null || toStop == null) continue;

            ResolvedLocation fromLoc = ResolvedLocation.virtual(
                    fromStop.stopName(),
                    fromStop.stopLat(), fromStop.stopLon(), "STATION"
            );
            ResolvedLocation toLoc = ResolvedLocation.virtual(
                    toStop.stopName(),
                    toStop.stopLat(), toStop.stopLon(), "STATION"
            );

            int fare = FARE_BY_ROUTE.getOrDefault(entry.routeId, 500);
            String currency = CURRENCY_BY_ROUTE.getOrDefault(entry.routeId, "EUR");

            results.add(new ResolvedEdge(
                    null, fromLoc, toLoc,
                    modeCode,
                    entry.agencyId,
                    entry.routeName + "_" + entry.tripId,
                    entry.departureTime,
                    entry.arrivalTime,
                    entry.durationMin,
                    entry.distanceM,
                    fare,
                    currency,
                    0,
                    "GTFS",
                    false,
                    Map.of()
            ));
        }

        log.info("GTFS resolve result: {} edges (skipped: origin={}, dest={}, mode={}, day={}, time={})",
                results.size(), skipOrigin, skipDest, skipMode, skipDay, skipTime);
        return results;
    }

    // ═══════════════════════════════════════════════════════════
    //  NEARBY STOP MATCHING
    // ═══════════════════════════════════════════════════════════

    private List<String> findNearbyStopIds(ResolvedLocation loc, double radiusM) {
        List<String> ids = new ArrayList<>();

        // First try exact IATA/code match
        if (loc.iataCode() != null && stopIndex.containsKey(loc.iataCode())) {
            log.info("GTFS findNearby: IATA exact match '{}' for {}", loc.iataCode(), loc.name());
            ids.add(loc.iataCode());
            return ids;
        }

        // Proximity search
        for (GtfsStop stop : stopIndex.values()) {
            double dist = haversineM(loc.lat(), loc.lon(), stop.stopLat(), stop.stopLon());
            if (dist <= radiusM) {
                log.info("GTFS findNearby: {} at ({},{}) dist={}m ≤ {}m — MATCH",
                        stop.stopId(), stop.stopLat(), stop.stopLon(), (int) dist, (int) radiusM);
                ids.add(stop.stopId());
            } else if (dist <= radiusM * 1.5) {
                log.info("GTFS findNearby: {} at ({},{}) dist={}m > {}m — NEAR MISS",
                        stop.stopId(), stop.stopLat(), stop.stopLon(), (int) dist, (int) radiusM);
            }
        }
        return ids;
    }

    // ═══════════════════════════════════════════════════════════
    //  PRE-BUILT EDGE INDEX
    // ═══════════════════════════════════════════════════════════

    private record GtfsEdgeEntry(
            String fromStopId, String toStopId,
            String routeId, String agencyId, String routeName, String modeCode, String tripId,
            LocalTime departureTime, LocalTime arrivalTime,
            int durationMin, int distanceM, short daysMask
    ) {}

    private List<GtfsEdgeEntry> buildEdgeEntries() {
        List<GtfsEdgeEntry> entries = new ArrayList<>();

        for (GtfsTrip trip : trips) {
            GtfsRoute route = routeIndex.get(trip.routeId());
            if (route == null) continue;

            GtfsCalendar cal = calendarIndex.get(trip.serviceId());
            short daysMask = cal != null ? cal.toBitmask() : 127;

            List<GtfsStopTime> times = stopTimesByTrip.getOrDefault(trip.tripId(), List.of());
            if (times.size() < 2) continue;

            times = times.stream()
                    .sorted(Comparator.comparingInt(GtfsStopTime::stopSequence))
                    .toList();

            // Generate spanning edges: every stop → every later stop on same trip
            // This allows boarding at any stop and alighting at any later stop
            for (int i = 0; i < times.size() - 1; i++) {
                GtfsStopTime from = times.get(i);
                GtfsStop fromStop = stopIndex.get(from.stopId());

                for (int j = i + 1; j < times.size(); j++) {
                    GtfsStopTime to = times.get(j);
                    GtfsStop toStop = stopIndex.get(to.stopId());

                    int dur = (int) java.time.Duration.between(from.departureTime(), to.arrivalTime()).toMinutes();
                    if (dur <= 0) dur = 2;

                    int dist = (fromStop != null && toStop != null)
                            ? (int) haversineM(fromStop.stopLat(), fromStop.stopLon(),
                            toStop.stopLat(), toStop.stopLon()) : 0;

                    entries.add(new GtfsEdgeEntry(
                            from.stopId(), to.stopId(),
                            trip.routeId(), route.agencyId(), route.routeShortName(), route.toTransportModeCode(), trip.tripId(),
                            from.departureTime(), to.arrivalTime(),
                            dur, dist, daysMask
                    ));
                }
            }
        }
        return entries;
    }

    private double haversineM(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6_371_000 * c;
    }

    private int dayOfWeekBit(DayOfWeek dow) {
        return 1 << (dow.getValue() - 1);
    }
}
