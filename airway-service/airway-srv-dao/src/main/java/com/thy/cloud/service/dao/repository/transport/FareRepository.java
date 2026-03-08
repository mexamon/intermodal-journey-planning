package com.thy.cloud.service.dao.repository.transport;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.transport.Fare;
import com.thy.cloud.service.dao.enums.EnumFareClass;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FareRepository extends GenericRepository<Fare, UUID> {

    @EntityGraph(attributePaths = {"edge", "edge.originLocation", "edge.destinationLocation",
            "edge.transportMode", "edge.provider", "trip"})
    List<Fare> findByEdgeId(UUID edgeId);

    @EntityGraph(attributePaths = {"edge", "edge.originLocation", "edge.destinationLocation",
            "edge.transportMode", "edge.provider", "trip"})
    @Query("SELECT f FROM Fare f WHERE f.deleted = false ORDER BY f.createdDate DESC")
    List<Fare> findAllActive();

    // Duplicate check: same edge + trip + fareClass (trip nullable)
    @Query("SELECT COUNT(f) > 0 FROM Fare f WHERE f.edge.id = :edgeId " +
            "AND (:tripId IS NULL AND f.trip IS NULL OR f.trip.id = :tripId) " +
            "AND f.fareClass = :fareClass AND f.deleted = false")
    boolean existsByEdgeIdAndTripIdAndFareClass(
            @Param("edgeId") UUID edgeId,
            @Param("tripId") UUID tripId,
            @Param("fareClass") EnumFareClass fareClass);

    // Check if the duplicate record is the fare being updated (self-check)
    @Query("SELECT COUNT(f) > 0 FROM Fare f WHERE f.id = :fareId " +
            "AND f.edge.id = :edgeId " +
            "AND (:tripId IS NULL AND f.trip IS NULL OR f.trip.id = :tripId) " +
            "AND f.fareClass = :fareClass")
    boolean isOwnedBy(
            @Param("fareId") UUID fareId,
            @Param("edgeId") UUID edgeId,
            @Param("tripId") UUID tripId,
            @Param("fareClass") EnumFareClass fareClass);
}
