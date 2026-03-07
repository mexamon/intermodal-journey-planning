package com.thy.cloud.service.dao.repository.inventory;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.inventory.RunwayProfile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RunwayProfileRepository extends GenericRepository<RunwayProfile, UUID> {

    List<RunwayProfile> findByLocationId(UUID locationId);
}
