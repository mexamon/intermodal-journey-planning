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
public class JourneyResult {

    private String id;
    private String label;                   // "En Hızlı", "En Ucuz", "En Yeşil"
    private List<JourneySegment> segments;
    private int totalDurationMin;           // sum of FLIGHT edge durations
    private int totalCostCents;
    private String currency;                // ISO code of the normalized currency
    private int co2Grams;
    private int transfers;
    private List<String> tags;              // ["fastest", "recommended"]
    private String departureTime;           // first FLIGHT departure: "06:30"
    private String arrivalTime;             // last FLIGHT arrival: "09:20"
    private String departureTimezone;       // IANA: Europe/Istanbul
    private String arrivalTimezone;         // IANA: Europe/London
}
