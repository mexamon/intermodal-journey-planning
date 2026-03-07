package com.thy.cloud.service.api.modules.transport.model;

import com.thy.cloud.service.dao.enums.EnumEdgeStatus;
import lombok.Data;

import java.util.UUID;

@Data
public class EdgeSearchRequest {

    private UUID originLocationId;
    private UUID destinationLocationId;
    private UUID transportModeId;
    private EnumEdgeStatus status;
}
