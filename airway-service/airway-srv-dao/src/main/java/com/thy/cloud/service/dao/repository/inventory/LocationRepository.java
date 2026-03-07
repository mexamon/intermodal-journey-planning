package com.thy.cloud.service.dao.repository.inventory;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.enums.EnumLocationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
}
