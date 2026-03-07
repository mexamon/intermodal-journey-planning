package com.thy.cloud.service.api.datasync.gtfs.model;

import java.time.LocalTime;

/**
 * GTFS stop_times.txt record — arrival/departure at a specific stop for a trip.
 */
public record GtfsStopTime(
        String tripId,
        LocalTime arrivalTime,
        LocalTime departureTime,
        String stopId,
        int stopSequence
) {}
