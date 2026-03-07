package com.thy.cloud.service.api.datasync.gtfs.model;

/**
 * GTFS trips.txt record — a specific journey along a route.
 */
public record GtfsTrip(
        String routeId,
        String serviceId,
        String tripId,
        String tripHeadsign,   // "Yenikapı"
        int directionId        // 0 or 1
) {}
