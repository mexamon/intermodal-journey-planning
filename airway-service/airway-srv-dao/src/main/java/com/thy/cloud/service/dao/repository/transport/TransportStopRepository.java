package com.thy.cloud.service.dao.repository.transport;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.transport.TransportStop;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransportStopRepository extends GenericRepository<TransportStop, UUID> {

    List<TransportStop> findByServiceAreaId(UUID serviceAreaId);

    List<TransportStop> findByLocationId(UUID locationId);
}
