package com.thy.cloud.service.api.modules.policy.model;

import lombok.Data;

@Data
public class NodeRequest {

    private String nodeKey;     // "START", "BEFORE", "FLIGHT", "AFTER", "END", "WALK_ACCESS"
    private Integer minVisits;
    private Integer maxVisits;
    private String propsJson;
    private Integer uiX;
    private Integer uiY;
}
