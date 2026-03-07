package com.thy.cloud.service.dao.repository.transport;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.transport.EdgeTrip;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EdgeTripRepository extends GenericRepository<EdgeTrip, UUID> {

    List<EdgeTrip> findByEdgeId(UUID edgeId);

    List<EdgeTrip> findByEdgeIdAndDeletedFalse(UUID edgeId);

    List<EdgeTrip> findByServiceCode(String serviceCode);
}
