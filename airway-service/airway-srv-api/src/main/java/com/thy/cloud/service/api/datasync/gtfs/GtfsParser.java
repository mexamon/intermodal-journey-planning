package com.thy.cloud.service.api.datasync.gtfs;

import com.thy.cloud.service.api.datasync.gtfs.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Parses GTFS ZIP files or individual CSV files into model objects.
 * <p>
 * Supports standard GTFS format: agency.txt, stops.txt, routes.txt,
 * trips.txt, stop_times.txt, calendar.txt.
 */
@Component
@Slf4j
public class GtfsParser {

    /**
     * Parse a GTFS ZIP input stream.
     */
    public GtfsFeed parseZip(InputStream zipStream) throws IOException {
        GtfsFeed feed = new GtfsFeed();

        try (ZipInputStream zis = new ZipInputStream(zipStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();
                // Read full content (don't close the ZipInputStream)
                String content = new String(zis.readAllBytes());

                switch (name) {
                    case "agency.txt" -> feed.agencies = parseAgencies(content);
                    case "stops.txt" -> feed.stops = parseStops(content);
                    case "routes.txt" -> feed.routes = parseRoutes(content);
                    case "trips.txt" -> feed.trips = parseTrips(content);
                    case "stop_times.txt" -> feed.stopTimes = parseStopTimes(content);
                    case "calendar.txt" -> feed.calendars = parseCalendars(content);
                    default -> log.debug("Skipping GTFS file: {}", name);
                }
            }
        }

        log.info("Parsed GTFS feed: {} agencies, {} stops, {} routes, {} trips, {} stop_times, {} calendars",
                feed.agencies.size(), feed.stops.size(), feed.routes.size(),
                feed.trips.size(), feed.stopTimes.size(), feed.calendars.size());
        return feed;
    }

    /**
     * Parse from individual CSV strings (for embedded/mock data).
     */
    public GtfsFeed parseFromCsvStrings(String agencyCsv, String stopsCsv, String routesCsv,
                                         String tripsCsv, String stopTimesCsv, String calendarCsv) {
        GtfsFeed feed = new GtfsFeed();
        if (agencyCsv != null) feed.agencies = parseAgencies(agencyCsv);
        if (stopsCsv != null) feed.stops = parseStops(stopsCsv);
        if (routesCsv != null) feed.routes = parseRoutes(routesCsv);
        if (tripsCsv != null) feed.trips = parseTrips(tripsCsv);
        if (stopTimesCsv != null) feed.stopTimes = parseStopTimes(stopTimesCsv);
        if (calendarCsv != null) feed.calendars = parseCalendars(calendarCsv);
        return feed;
    }

    // ═══════════════════════════════════════════════════════════
    //  CSV PARSERS
    // ═══════════════════════════════════════════════════════════

    private List<GtfsAgency> parseAgencies(String csv) {
        return parseCsv(csv, cols -> {
            return new GtfsAgency(
                    cols.getOrDefault("agency_id", ""),
                    cols.getOrDefault("agency_name", ""),
                    cols.getOrDefault("agency_url", ""),
                    cols.getOrDefault("agency_timezone", ""),
                    cols.getOrDefault("agency_lang", "")
            );
        });
    }

    private List<GtfsStop> parseStops(String csv) {
        return parseCsv(csv, cols -> {
            return new GtfsStop(
                    cols.getOrDefault("stop_id", ""),
                    cols.getOrDefault("stop_name", ""),
                    parseDouble(cols.get("stop_lat")),
                    parseDouble(cols.get("stop_lon")),
                    parseInt(cols.get("location_type"), 0)
            );
        });
    }

    private List<GtfsRoute> parseRoutes(String csv) {
        return parseCsv(csv, cols -> {
            return new GtfsRoute(
                    cols.getOrDefault("route_id", ""),
                    cols.getOrDefault("agency_id", ""),
                    cols.getOrDefault("route_short_name", ""),
                    cols.getOrDefault("route_long_name", ""),
                    parseInt(cols.get("route_type"), 3)
            );
        });
    }

    private List<GtfsTrip> parseTrips(String csv) {
        return parseCsv(csv, cols -> {
            return new GtfsTrip(
                    cols.getOrDefault("route_id", ""),
                    cols.getOrDefault("service_id", ""),
                    cols.getOrDefault("trip_id", ""),
                    cols.getOrDefault("trip_headsign", ""),
                    parseInt(cols.get("direction_id"), 0)
            );
        });
    }

    private List<GtfsStopTime> parseStopTimes(String csv) {
        return parseCsv(csv, cols -> {
            return new GtfsStopTime(
                    cols.getOrDefault("trip_id", ""),
                    parseTime(cols.get("arrival_time")),
                    parseTime(cols.get("departure_time")),
                    cols.getOrDefault("stop_id", ""),
                    parseInt(cols.get("stop_sequence"), 0)
            );
        });
    }

    private List<GtfsCalendar> parseCalendars(String csv) {
        return parseCsv(csv, cols -> {
            return new GtfsCalendar(
                    cols.getOrDefault("service_id", ""),
                    "1".equals(cols.get("monday")),
                    "1".equals(cols.get("tuesday")),
                    "1".equals(cols.get("wednesday")),
                    "1".equals(cols.get("thursday")),
                    "1".equals(cols.get("friday")),
                    "1".equals(cols.get("saturday")),
                    "1".equals(cols.get("sunday")),
                    cols.getOrDefault("start_date", ""),
                    cols.getOrDefault("end_date", "")
            );
        });
    }

    // ═══════════════════════════════════════════════════════════
    //  GENERIC CSV PARSER
    // ═══════════════════════════════════════════════════════════

    @FunctionalInterface
    interface RowMapper<T> {
        T map(Map<String, String> columns);
    }

    private <T> List<T> parseCsv(String csv, RowMapper<T> mapper) {
        if (csv == null || csv.isBlank()) return List.of();

        List<T> results = new ArrayList<>();
        String[] lines = csv.split("\n");
        if (lines.length < 2) return results;

        // Header line
        String[] headers = lines[0].trim().replace("\r", "").split(",");
        for (int i = 0; i < headers.length; i++) {
            headers[i] = headers[i].trim().replace("\"", "").toLowerCase();
        }

        // Data lines
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim().replace("\r", "");
            if (line.isEmpty()) continue;

            String[] values = line.split(",", -1);
            Map<String, String> cols = new HashMap<>();
            for (int j = 0; j < headers.length && j < values.length; j++) {
                cols.put(headers[j], values[j].trim().replace("\"", ""));
            }

            try {
                results.add(mapper.map(cols));
            } catch (Exception e) {
                log.warn("Error parsing GTFS line {}: {}", i, e.getMessage());
            }
        }
        return results;
    }

    private double parseDouble(String s) {
        if (s == null || s.isBlank()) return 0;
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0; }
    }

    private int parseInt(String s, int defaultVal) {
        if (s == null || s.isBlank()) return defaultVal;
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return defaultVal; }
    }

    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return LocalTime.MIDNIGHT;
        try {
            String[] parts = s.trim().split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int sec = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            // GTFS allows hours > 23 for trips crossing midnight
            return LocalTime.of(h % 24, m, sec);
        } catch (Exception e) {
            return LocalTime.MIDNIGHT;
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  PARSED FEED CONTAINER
    // ═══════════════════════════════════════════════════════════

    public static class GtfsFeed {
        public List<GtfsAgency> agencies = new ArrayList<>();
        public List<GtfsStop> stops = new ArrayList<>();
        public List<GtfsRoute> routes = new ArrayList<>();
        public List<GtfsTrip> trips = new ArrayList<>();
        public List<GtfsStopTime> stopTimes = new ArrayList<>();
        public List<GtfsCalendar> calendars = new ArrayList<>();
    }
}
