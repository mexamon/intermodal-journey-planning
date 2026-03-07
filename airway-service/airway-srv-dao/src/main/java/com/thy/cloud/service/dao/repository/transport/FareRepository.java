package com.thy.cloud.service.dao.repository.transport;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.transport.Fare;

import java.util.List;
import java.util.UUID;

public interface FareRepository extends GenericRepository<Fare, UUID> {

    List<Fare> findByEdgeId(UUID edgeId);
}
