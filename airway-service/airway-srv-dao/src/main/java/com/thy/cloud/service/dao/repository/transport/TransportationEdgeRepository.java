package com.thy.cloud.service.dao.repository.transport;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.transport.TransportationEdge;
import com.thy.cloud.service.dao.enums.EnumEdgeStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransportationEdgeRepository extends GenericRepository<TransportationEdge, UUID> {

    List<TransportationEdge> findByOriginLocationId(UUID originLocationId);

    List<TransportationEdge> findByOriginLocationIdAndStatus(UUID originLocationId, EnumEdgeStatus status);

    List<TransportationEdge> findByOriginLocationIdAndDestinationLocationId(UUID originId, UUID destinationId);

    List<TransportationEdge> findAllByStatusAndDeletedFalse(EnumEdgeStatus status);
}
