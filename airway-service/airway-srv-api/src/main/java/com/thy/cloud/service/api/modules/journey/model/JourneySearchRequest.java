package com.thy.cloud.service.api.modules.journey.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
public class JourneySearchRequest {

    private UUID originLocationId;
    private String originIataCode;

    private UUID destinationLocationId;
    private String destinationIataCode;

    private LocalDate departureDate;
    private LocalTime earliestDeparture;

    private int maxTransfers = 4;
    private int maxDurationMinutes = 1440; // 24h

    private List<String> preferredModes; // optional filter: ["FLIGHT","TRAIN"]

    private String sortBy = "FASTEST"; // FASTEST, CHEAPEST, GREENEST, FEWEST_TRANSFERS
}
