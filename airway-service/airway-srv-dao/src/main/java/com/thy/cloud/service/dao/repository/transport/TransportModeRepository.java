package com.thy.cloud.service.dao.repository.transport;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.transport.TransportMode;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransportModeRepository extends GenericRepository<TransportMode, UUID> {

    Optional<TransportMode> findByCode(String code);

    List<TransportMode> findByIsActiveTrueOrderBySortOrderAsc();
}
