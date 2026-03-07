package com.thy.cloud.service.api.modules.inventory.service;

import com.thy.cloud.service.api.modules.inventory.model.LocationSearchRequest;
import com.thy.cloud.service.api.modules.inventory.specs.LocationSpecs;
import com.thy.cloud.service.dao.entity.inventory.AirportProfile;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.entity.inventory.Provider;
import com.thy.cloud.service.dao.repository.inventory.AirportProfileRepository;
import com.thy.cloud.service.dao.repository.inventory.LocationRepository;
import com.thy.cloud.service.dao.repository.inventory.ProviderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final LocationRepository locationRepository;
    private final AirportProfileRepository airportProfileRepository;
    private final ProviderRepository providerRepository;

    // ── Location ──────────────────────────────────────────────

    @Override
    public Page<Location> searchLocations(LocationSearchRequest request, Pageable pageable) {
        return locationRepository.findAll(LocationSpecs.filter(request), pageable);
    }

    @Override
    public Location getLocation(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found: " + id));
    }

    @Override
    public Location getLocationByIata(String iataCode) {
        return locationRepository.findByIataCode(iataCode)
                .orElseThrow(() -> new EntityNotFoundException("Location not found for IATA: " + iataCode));
    }

    // ── Airport ───────────────────────────────────────────────

    @Override
    public AirportProfile getAirportProfile(UUID locationId) {
        return airportProfileRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("Airport profile not found: " + locationId));
    }

    @Override
    public List<AirportProfile> getScheduledAirports() {
        return airportProfileRepository.findByScheduledServiceTrue();
    }

    // ── Provider ──────────────────────────────────────────────

    @Override
    public List<Provider> listProviders() {
        return providerRepository.findAll();
    }

    @Override
    public Provider getProvider(UUID id) {
        return providerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Provider not found: " + id));
    }

    @Override
    public Provider getProviderByCode(String code) {
        return providerRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Provider not found for code: " + code));
    }
}
