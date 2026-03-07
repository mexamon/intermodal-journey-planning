package com.thy.cloud.service.api.modules.inventory.service;

import com.thy.cloud.service.api.modules.inventory.model.LocationSearchRequest;
import com.thy.cloud.service.dao.entity.inventory.AirportProfile;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.entity.inventory.Provider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface InventoryService {

    // Location
    Page<Location> searchLocations(LocationSearchRequest request, Pageable pageable);

    Location getLocation(UUID id);

    Location getLocationByIata(String iataCode);

    // Airport
    AirportProfile getAirportProfile(UUID locationId);

    List<AirportProfile> getScheduledAirports();

    // Provider
    List<Provider> listProviders();

    Provider getProvider(UUID id);

    Provider getProviderByCode(String code);
}
