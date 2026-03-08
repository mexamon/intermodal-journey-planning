package com.thy.cloud.service.dao.repository.transport;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.transport.TransportServiceArea;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransportServiceAreaRepository extends GenericRepository<TransportServiceArea, UUID> {

    List<TransportServiceArea> findByTransportModeId(UUID transportModeId);

    List<TransportServiceArea> findByTransportModeIdAndIsActiveTrue(UUID transportModeId);

    List<TransportServiceArea> findByIsActiveTrueAndCountryIsoCode(String countryIsoCode);

    List<TransportServiceArea> findByIsActiveTrue();

    boolean existsByProviderId(UUID providerId);
}
