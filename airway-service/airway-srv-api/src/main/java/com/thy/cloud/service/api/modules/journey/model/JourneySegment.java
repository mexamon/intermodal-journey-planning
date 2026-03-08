package com.thy.cloud.service.api.modules.journey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JourneySegment {

    private String mode;                // FLIGHT, TRAIN, BUS, SUBWAY, FERRY, UBER, WALKING
    private String originCode;          // IST, FRA_HBF
    private String originName;
    private String destinationCode;
    private String destinationName;
    private String departureTime;       // HH:mm
    private String arrivalTime;         // HH:mm
    private int durationMin;
    private String serviceCode;         // TK1591, ICE 1537
    private String provider;            // Turkish Airlines, Deutsche Bahn
    private int costCents;
    private String currency;            // ISO currency code: EUR, TRY, USD
    private String edgeId;              // link to transportation_edge
    private String tripId;              // link to edge_trip (if FIXED)
    private String originTimezone;      // IANA: Europe/Istanbul
    private String destinationTimezone; // IANA: Europe/London
}
