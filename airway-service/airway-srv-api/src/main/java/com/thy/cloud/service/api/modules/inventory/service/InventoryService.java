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

    Location saveLocation(Location location);

    void deleteLocation(UUID id);

    // Airport
    AirportProfile getAirportProfile(UUID locationId);

    List<AirportProfile> getScheduledAirports();

    record AirportLookupDto(String iata, String name, String city, String country) {}
    List<AirportLookupDto> lookupAirports(String query);

    // Provider
    List<Provider> listProviders();

    Provider getProvider(UUID id);

    Provider getProviderByCode(String code);

    Provider saveProvider(Provider provider);

    void deleteProvider(UUID id);
}
