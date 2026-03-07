package com.thy.cloud.service.api.datasync.gtfs.model;

/**
 * GTFS stops.txt record — boarding/alighting location.
 */
public record GtfsStop(
        String stopId,
        String stopName,
        double stopLat,
        double stopLon,
        int locationType  // 0=stop, 1=station
) {}
