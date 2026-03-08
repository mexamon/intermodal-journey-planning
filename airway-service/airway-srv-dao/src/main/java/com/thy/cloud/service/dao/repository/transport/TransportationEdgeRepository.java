package com.thy.cloud.service.dao.repository.transport;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.transport.TransportationEdge;
import com.thy.cloud.service.dao.enums.EnumEdgeStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransportationEdgeRepository extends GenericRepository<TransportationEdge, UUID> {

    List<TransportationEdge> findByOriginLocationId(UUID originLocationId);

    @Query("SELECT e FROM TransportationEdge e " +
           "JOIN FETCH e.transportMode " +
           "LEFT JOIN FETCH e.provider " +
           "JOIN FETCH e.originLocation " +
           "JOIN FETCH e.destinationLocation " +
           "WHERE e.originLocation.id = :originId AND e.status = :status")
    List<TransportationEdge> findByOriginLocationIdAndStatus(
            @Param("originId") UUID originLocationId,
            @Param("status") EnumEdgeStatus status);

    List<TransportationEdge> findByOriginLocationIdAndDestinationLocationId(UUID originId, UUID destinationId);

    List<TransportationEdge> findAllByStatusAndDeletedFalse(EnumEdgeStatus status);

    boolean existsByProviderId(UUID providerId);

    @Query("SELECT e FROM TransportationEdge e " +
           "JOIN FETCH e.originLocation " +
           "JOIN FETCH e.destinationLocation " +
           "JOIN FETCH e.transportMode " +
           "LEFT JOIN FETCH e.provider " +
           "LEFT JOIN FETCH e.trips " +
           "WHERE e.deleted = false ORDER BY e.createdDate DESC")
    List<TransportationEdge> findAllWithRelations();
}
