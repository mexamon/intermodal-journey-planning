package com.thy.cloud.service.dao.repository.inventory;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.inventory.AirportProfile;
import com.thy.cloud.service.dao.enums.EnumAirportKind;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AirportProfileRepository extends GenericRepository<AirportProfile, UUID> {

    List<AirportProfile> findByAirportKind(EnumAirportKind airportKind);

    List<AirportProfile> findByScheduledServiceTrue();
}
