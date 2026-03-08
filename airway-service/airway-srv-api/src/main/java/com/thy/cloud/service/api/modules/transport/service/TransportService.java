package com.thy.cloud.service.api.modules.transport.service;

import com.thy.cloud.service.api.modules.transport.model.EdgeSearchRequest;
import com.thy.cloud.service.dao.entity.transport.TransportMode;
import com.thy.cloud.service.dao.entity.transport.TransportServiceArea;
import com.thy.cloud.service.dao.entity.transport.TransportStop;
import com.thy.cloud.service.dao.entity.transport.TransportationEdge;
import com.thy.cloud.service.dao.entity.transport.Fare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface TransportService {

    // Mode
    List<TransportMode> listActiveModes();
    List<TransportMode> listAllModes();
    TransportMode getMode(UUID id);
    TransportMode getModeByCode(String code);
    TransportMode saveMode(TransportMode mode);

    // Service Area
    List<TransportServiceArea> listServiceAreas(UUID modeId);
    List<TransportServiceArea> listAllServiceAreas();
    TransportServiceArea getServiceArea(UUID id);
    TransportServiceArea saveServiceArea(TransportServiceArea area);
    void deleteServiceArea(UUID id);

    // Stops
    List<TransportStop> listStopsByServiceArea(UUID serviceAreaId);

    // Edges
    Page<TransportationEdge> searchEdges(EdgeSearchRequest request, Pageable pageable);
    List<TransportationEdge> getEdgesFromOrigin(UUID originId);
    List<TransportationEdge> listAllEdges();

    // Fares
    List<Fare> listAllFares();
    List<Fare> listFaresByEdge(UUID edgeId);
    Fare getFare(UUID id);
    Fare saveFare(Fare fare);
    void deleteFare(UUID id);
}

