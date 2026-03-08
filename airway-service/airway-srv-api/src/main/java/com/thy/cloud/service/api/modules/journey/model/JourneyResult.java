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
    private int totalDurationMin;
    private int totalCostCents;
    private String currency;                // ISO code of the normalized currency
    private int co2Grams;
    private int transfers;
    private List<String> tags;              // ["fastest", "recommended"]
}
