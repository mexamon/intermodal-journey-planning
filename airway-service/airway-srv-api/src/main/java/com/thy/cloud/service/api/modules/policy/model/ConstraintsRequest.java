package com.thy.cloud.service.api.modules.policy.model;

import lombok.Data;

@Data
public class ConstraintsRequest {

    private Integer maxLegs;
    private Integer minFlights;
    private Integer maxFlights;
    private Integer minTransfers;
    private Integer maxTransfers;
    private Integer maxTotalDurationMin;
    private Integer maxWalkingTotalM;
    private Integer minConnectionMinutes;
    private Integer maxTotalCo2Grams;
    private String preferredModesJson;
    private String constraintsJson;
}
