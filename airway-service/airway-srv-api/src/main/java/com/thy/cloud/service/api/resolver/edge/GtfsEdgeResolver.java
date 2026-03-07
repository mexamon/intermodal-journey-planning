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
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Live GTFS edge resolver — reads embedded Istanbul Metro data at search time.
 * No DB writes. Data loaded into memory once, queried on every search.
 * <p>
 * The GTFS data represents a live transit feed — like reading from a real-time
 * GTFS feed URL. The parsed data stays in memory and is queried directly.
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

    @PostConstruct
    void loadFeed() {
        GtfsFeed feed = buildSampleFeed();
        stopIndex = feed.stops.stream().collect(Collectors.toMap(GtfsStop::stopId, s -> s));
        routeIndex = feed.routes.stream().collect(Collectors.toMap(GtfsRoute::routeId, r -> r));
        calendarIndex = feed.calendars.stream().collect(Collectors.toMap(GtfsCalendar::serviceId, c -> c));
        stopTimesByTrip = feed.stopTimes.stream().collect(Collectors.groupingBy(GtfsStopTime::tripId));
        trips = feed.trips;

        // Pre-build edge entries for fast querying
        edgeEntries = buildEdgeEntries();
        log.info("GtfsEdgeResolver: loaded {} stops, {} routes, {} edge entries",
                stopIndex.size(), routeIndex.size(), edgeEntries.size());
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

        // Find matching GTFS stops near origin
        List<String> originStopIds = findNearbyStopIds(origin, 1500); // 1.5km radius
        if (originStopIds.isEmpty()) return List.of();

        List<String> destStopIds = destination != null
                ? findNearbyStopIds(destination, 1500) : List.of();

        int dayBit = context.travelDate() != null
                ? dayOfWeekBit(context.travelDate().getDayOfWeek()) : 127;

        List<ResolvedEdge> results = new ArrayList<>();

        for (GtfsEdgeEntry entry : edgeEntries) {
            // Filter by origin stop
            if (!originStopIds.contains(entry.fromStopId)) continue;

            // Filter by destination stop (if specified)
            if (!destStopIds.isEmpty() && !destStopIds.contains(entry.toStopId)) continue;

            // Filter by transport mode
            String modeCode = entry.modeCode;
            if (!context.isModeAllowed(modeCode)) continue;

            // Filter by operating day
            if ((entry.daysMask & dayBit) == 0) continue;

            // Filter by time
            if (context.preferredTime() != null
                    && entry.departureTime.isBefore(context.preferredTime())) continue;

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

            results.add(new ResolvedEdge(
                    null, fromLoc, toLoc,
                    modeCode,
                    entry.routeName,
                    entry.routeName + "_" + entry.tripId,
                    entry.departureTime,
                    entry.arrivalTime,
                    entry.durationMin,
                    entry.distanceM,
                    750,  // ₺7.50 flat fare
                    0,    // electric metro = zero CO₂
                    "GTFS",
                    false,
                    Map.of()
            ));
        }

        log.debug("GtfsEdgeResolver: {} edges from origin near ({}, {})",
                results.size(), origin.lat(), origin.lon());
        return results;
    }

    // ═══════════════════════════════════════════════════════════
    //  NEARBY STOP MATCHING
    // ═══════════════════════════════════════════════════════════

    private List<String> findNearbyStopIds(ResolvedLocation loc, double radiusM) {
        List<String> ids = new ArrayList<>();

        // First try exact IATA/code match
        if (loc.iataCode() != null && stopIndex.containsKey(loc.iataCode())) {
            ids.add(loc.iataCode());
            return ids;
        }

        // Proximity search
        for (GtfsStop stop : stopIndex.values()) {
            double dist = haversineM(loc.lat(), loc.lon(), stop.stopLat(), stop.stopLon());
            if (dist <= radiusM) {
                ids.add(stop.stopId());
            }
        }
        return ids;
    }

    // ═══════════════════════════════════════════════════════════
    //  PRE-BUILT EDGE INDEX
    // ═══════════════════════════════════════════════════════════

    private record GtfsEdgeEntry(
            String fromStopId, String toStopId,
            String routeName, String modeCode, String tripId,
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

            for (int i = 0; i < times.size() - 1; i++) {
                GtfsStopTime from = times.get(i);
                GtfsStopTime to = times.get(i + 1);

                int dur = (int) java.time.Duration.between(from.departureTime(), to.arrivalTime()).toMinutes();
                if (dur <= 0) dur = 2;

                GtfsStop fromStop = stopIndex.get(from.stopId());
                GtfsStop toStop = stopIndex.get(to.stopId());
                int dist = (fromStop != null && toStop != null)
                        ? (int) haversineM(fromStop.stopLat(), fromStop.stopLon(),
                        toStop.stopLat(), toStop.stopLon()) : 0;

                entries.add(new GtfsEdgeEntry(
                        from.stopId(), to.stopId(),
                        route.routeShortName(), route.toTransportModeCode(), trip.tripId(),
                        from.departureTime(), to.arrivalTime(),
                        dur, dist, daysMask
                ));
            }
        }
        return entries;
    }

    // ═══════════════════════════════════════════════════════════
    //  EMBEDDED SAMPLE DATA: Istanbul Metro M1, M2, Marmaray
    // ═══════════════════════════════════════════════════════════

    private GtfsFeed buildSampleFeed() {
        String agencies = """
                agency_id,agency_name,agency_url,agency_timezone,agency_lang
                METRO_IST,Metro İstanbul,https://www.metro.istanbul,Europe/Istanbul,tr
                """;

        String stops = """
                stop_id,stop_name,stop_lat,stop_lon,location_type
                M1_YKP,Yenikapı,41.005547,28.949520,1
                M1_AKS,Aksaray,41.011570,28.951700,1
                M1_EMN,Emniyet-Fatih,41.019200,28.937800,1
                M1_TPC,Topkapı-Ulubatlı,41.023100,28.927500,1
                M1_BYP,Bayrampaşa-Maltepe,41.031400,28.910400,1
                M1_SAG,Sağmalcılar,41.037500,28.896300,1
                M1_KCP,Kocatepe,41.040500,28.882100,1
                M1_OPT,Otogar,41.042200,28.872800,1
                M1_DTL,Davutpaşa-YTÜ,41.046600,28.856300,1
                M1_MRC,Merter,41.049300,28.838200,1
                M1_ZYB,Zeytinburnu,41.045400,28.824400,1
                M1_BKR,Bakırköy-İDO,41.045600,28.818900,1
                M1_BSE,Bahçelievler,41.047500,28.800600,1
                M1_ATR,Atatürk Havalimanı,40.976600,28.820400,1
                M2_YKP,Yenikapı M2,41.005547,28.949520,1
                M2_VEZ,Vezneciler-İÜ,41.011400,28.959600,1
                M2_HLC,Haliç,41.020600,28.970600,1
                M2_SNT,Şişhane,41.026600,28.976500,1
                M2_TAK,Taksim,41.036600,28.985000,1
                M2_OSM,Osmanbey,41.046100,28.988100,1
                M2_SIS,Şişli-Mecidiyeköy,41.059600,28.991200,1
                M2_GYR,Gayrettepe,41.068400,28.980900,1
                M2_LEV,Levent,41.077800,28.969700,1
                M2_4LV,4.Levent,41.087200,28.972700,1
                M2_SAN,Sanayi Mahallesi,41.101700,28.974800,1
                M2_HOS,Hacıosman,41.108200,28.978800,1
                MR_YKP,Yenikapı Marmaray,41.005547,28.949520,1
                MR_SRK,Sirkeci,41.015400,28.977700,1
                MR_USK,Üsküdar,41.025400,29.015100,1
                MR_AYR,Ayrılık Çeşmesi,41.022300,29.029200,1
                MR_ACS,Acıbadem,41.014400,29.043400,1
                MR_KZT,Kozyatağı,41.000500,29.062600,1
                MR_GZT,Göztepe,40.990300,29.050700,1
                MR_ERN,Erenköy,40.976600,29.067800,1
                MR_BOS,Bostancı,40.966100,29.093100,1
                """;

        String routes = """
                route_id,agency_id,route_short_name,route_long_name,route_type
                M1,METRO_IST,M1,Yenikapı - Atatürk Havalimanı,1
                M2,METRO_IST,M2,Yenikapı - Hacıosman,1
                MR,METRO_IST,Marmaray,Halkalı - Gebze (İstanbul bölümü),2
                """;

        StringBuilder tripsCsv = new StringBuilder("route_id,service_id,trip_id,trip_headsign,direction_id\n");
        StringBuilder stopTimesCsv = new StringBuilder("trip_id,arrival_time,departure_time,stop_id,stop_sequence\n");
        int tripNum = 1;

        String[] m1Stops = {"M1_YKP","M1_AKS","M1_EMN","M1_TPC","M1_BYP","M1_SAG","M1_KCP",
                "M1_OPT","M1_DTL","M1_MRC","M1_ZYB","M1_BKR","M1_BSE","M1_ATR"};
        for (int hour = 6; hour <= 23; hour++) {
            for (int startMin = 0; startMin < 60; startMin += 10) {
                String tid = "M1_D0_" + tripNum++;
                tripsCsv.append(String.format("M1,WEEKDAY,%s,Atatürk Havalimanı,0%n", tid));
                addStopTimes(stopTimesCsv, tid, m1Stops, hour, startMin, 3);
            }
        }
        String[] m1Rev = reverse(m1Stops);
        for (int hour = 6; hour <= 23; hour++) {
            for (int startMin = 5; startMin < 60; startMin += 10) {
                String tid = "M1_D1_" + tripNum++;
                tripsCsv.append(String.format("M1,WEEKDAY,%s,Yenikapı,1%n", tid));
                addStopTimes(stopTimesCsv, tid, m1Rev, hour, startMin, 3);
            }
        }

        String[] m2Stops = {"M2_YKP","M2_VEZ","M2_HLC","M2_SNT","M2_TAK","M2_OSM",
                "M2_SIS","M2_GYR","M2_LEV","M2_4LV","M2_SAN","M2_HOS"};
        for (int hour = 6; hour <= 23; hour++) {
            for (int startMin = 0; startMin < 60; startMin += 8) {
                String tid = "M2_D0_" + tripNum++;
                tripsCsv.append(String.format("M2,WEEKDAY,%s,Hacıosman,0%n", tid));
                addStopTimes(stopTimesCsv, tid, m2Stops, hour, startMin, 3);
            }
        }
        String[] m2Rev = reverse(m2Stops);
        for (int hour = 6; hour <= 23; hour++) {
            for (int startMin = 4; startMin < 60; startMin += 8) {
                String tid = "M2_D1_" + tripNum++;
                tripsCsv.append(String.format("M2,WEEKDAY,%s,Yenikapı M2,1%n", tid));
                addStopTimes(stopTimesCsv, tid, m2Rev, hour, startMin, 3);
            }
        }

        String[] mrStops = {"MR_YKP","MR_SRK","MR_USK","MR_AYR","MR_ACS","MR_KZT",
                "MR_GZT","MR_ERN","MR_BOS"};
        for (int hour = 6; hour <= 23; hour++) {
            for (int startMin = 0; startMin < 60; startMin += 10) {
                String tid = "MR_D0_" + tripNum++;
                tripsCsv.append(String.format("MR,WEEKDAY,%s,Bostancı,0%n", tid));
                addStopTimes(stopTimesCsv, tid, mrStops, hour, startMin, 4);
            }
        }
        String[] mrRev = reverse(mrStops);
        for (int hour = 6; hour <= 23; hour++) {
            for (int startMin = 5; startMin < 60; startMin += 10) {
                String tid = "MR_D1_" + tripNum++;
                tripsCsv.append(String.format("MR,WEEKDAY,%s,Yenikapı Marmaray,1%n", tid));
                addStopTimes(stopTimesCsv, tid, mrRev, hour, startMin, 4);
            }
        }

        String calendar = """
                service_id,monday,tuesday,wednesday,thursday,friday,saturday,sunday,start_date,end_date
                WEEKDAY,1,1,1,1,1,1,1,20260101,20261231
                """;

        return gtfsParser.parseFromCsvStrings(agencies, stops, routes,
                tripsCsv.toString(), stopTimesCsv.toString(), calendar);
    }

    private void addStopTimes(StringBuilder sb, String tripId, String[] stops,
                               int startHour, int startMin, int intervalMin) {
        int curMin = startHour * 60 + startMin;
        for (int i = 0; i < stops.length; i++) {
            int h = (curMin / 60) % 24;
            int m = curMin % 60;
            String time = String.format("%02d:%02d:00", h, m);
            sb.append(String.format("%s,%s,%s,%s,%d%n", tripId, time, time, stops[i], i + 1));
            curMin += intervalMin;
        }
    }

    private String[] reverse(String[] arr) {
        String[] rev = new String[arr.length];
        for (int i = 0; i < arr.length; i++) rev[i] = arr[arr.length - 1 - i];
        return rev;
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
