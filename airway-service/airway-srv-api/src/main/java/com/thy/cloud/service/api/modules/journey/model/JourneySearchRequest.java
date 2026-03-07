package com.thy.cloud.service.api.modules.journey.model;

import com.thy.cloud.service.api.modules.journey.validation.JourneySearchValidation;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@JourneySearchValidation
public class JourneySearchRequest {

    private UUID originLocationId;
    private String originIataCode;
    private String originQuery;
    private Double originLat;
    private Double originLon;
    private String originType;           // "airport" | "station" | "place"

    private UUID destinationLocationId;
    private String destinationIataCode;
    private String destinationQuery;
    private Double destinationLat;
    private Double destinationLon;
    private String destinationType;      // "airport" | "station" | "place"

    private LocalDate departureDate;
    private LocalTime earliestDeparture;

    private int maxTransfers = 4;
    private int maxDurationMinutes = 1440; // 24h

    private List<String> preferredModes; // optional filter: ["FLIGHT","TRAIN"]

    private String sortBy = "FASTEST"; // FASTEST, CHEAPEST, GREENEST, FEWEST_TRANSFERS
}

