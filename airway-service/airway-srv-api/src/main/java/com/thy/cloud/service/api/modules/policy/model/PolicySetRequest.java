package com.thy.cloud.service.api.modules.policy.model;

import lombok.Data;

import java.util.Date;

@Data
public class PolicySetRequest {

    private String code;
    private String scopeType;   // "GLOBAL", "COUNTRY", "REGION", "AIRPORT", "AIRPORT_PAIR"
    private String scopeKey;
    private String segment;     // "DEFAULT", "CORPORATE", "ELITE", "IRROPS"
    private String status;      // "DRAFT", "ACTIVE", "DEPRECATED"
    private String description;
    private Date effectiveFrom;
    private Date effectiveTo;
}
