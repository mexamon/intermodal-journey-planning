package com.thy.cloud.service.dao.repository.inventory;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.enums.EnumLocationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LocationRepository extends GenericRepository<Location, UUID> {

    Optional<Location> findByIataCode(String iataCode);

    Optional<Location> findByIcaoCode(String icaoCode);

    List<Location> findByType(EnumLocationType type);

    Page<Location> findByIsSearchableTrueOrderBySearchPriorityDesc(Pageable pageable);

    @Query("SELECT l FROM Location l WHERE l.type = :type AND l.iataCode IS NOT NULL AND l.lat BETWEEN :minLat AND :maxLat AND l.lon BETWEEN :minLon AND :maxLon")
    List<Location> findNearbyByType(EnumLocationType type, double minLat, double maxLat, double minLon, double maxLon);

    @Query("SELECT l FROM Location l WHERE l.type = com.thy.cloud.service.dao.enums.EnumLocationType.AIRPORT AND l.iataCode IS NOT NULL ORDER BY l.searchPriority DESC")
    List<Location> findAllAirports();

    @Query("SELECT l FROM Location l WHERE l.type = com.thy.cloud.service.dao.enums.EnumLocationType.AIRPORT AND l.iataCode IS NOT NULL AND (LOWER(l.name) LIKE :query OR LOWER(l.iataCode) LIKE :query OR LOWER(l.city) LIKE :query) ORDER BY l.searchPriority DESC")
    List<Location> findAirportsByQuery(String query);
}
