package com.thy.cloud.service.api.modules.policy.model;

import lombok.Data;

import java.util.UUID;

@Data
public class TransitionRequest {

    private UUID fromNodeId;
    private UUID toNodeId;
    private Integer priority;
    private String guardJson;
    private String uiJson;
}
