package com.thy.cloud.service.api.modules.inventory.service;

import com.thy.cloud.service.api.modules.inventory.model.LocationSearchRequest;
import com.thy.cloud.service.api.modules.inventory.specs.LocationSpecs;
import com.thy.cloud.service.api.resolver.location.LocationResolver;
import com.thy.cloud.service.api.resolver.model.ResolvedLocation;
import com.thy.cloud.service.dao.entity.inventory.AirportProfile;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.entity.inventory.Provider;
import com.thy.cloud.service.dao.enums.EnumLocationType;
import com.thy.cloud.service.dao.enums.EnumLocationSource;
import com.thy.cloud.service.dao.repository.inventory.AirportProfileRepository;
import com.thy.cloud.service.dao.repository.inventory.LocationRepository;
import com.thy.cloud.service.dao.repository.inventory.ProviderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryServiceImpl implements InventoryService {

    private final LocationRepository locationRepository;
    private final AirportProfileRepository airportProfileRepository;
    private final ProviderRepository providerRepository;
    private final List<LocationResolver> locationResolvers;

    // ── Location ──────────────────────────────────────────────

    @Override
    public Page<Location> searchLocations(LocationSearchRequest request, Pageable pageable) {
        // 1. Always search DB
        Page<Location> dbResults = locationRepository.findAll(LocationSpecs.filter(request), pageable);
        List<Location> merged = new ArrayList<>(dbResults.getContent());

        // 2. Always also search Google Places (if query text exists)
        String query = request.getName();
        if (query != null && !query.isBlank()) {
            for (LocationResolver resolver : locationResolvers) {
                if ("DB".equals(resolver.getSource())) continue;

                try {
                    List<ResolvedLocation> external = resolver.resolve(query, 5);
                    for (ResolvedLocation rl : external) {
                        // De-duplicate: skip if same name or same coordinates already in results
                        boolean isDup = merged.stream().anyMatch(l ->
                                l.getName().equalsIgnoreCase(rl.name())
                                || (l.getLat() != null && rl.lat() != 0
                                    && Math.abs(l.getLat().doubleValue() - rl.lat()) < 0.001
                                    && Math.abs(l.getLon().doubleValue() - rl.lon()) < 0.001));
                        if (isDup) continue;

                        Location loc = new Location();
                        loc.setId(UUID.randomUUID());
                        loc.setName(rl.name());
                        loc.setLat(BigDecimal.valueOf(rl.lat()));
                        loc.setLon(BigDecimal.valueOf(rl.lon()));
                        loc.setType(mapLocationType(rl.type()));
                        loc.setSource(EnumLocationSource.GOOGLE_PLACES);
                        loc.setIsSearchable(true);
                        loc.setSearchPriority(10); // lower than DB results
                        merged.add(loc);
                    }
                } catch (Exception e) {
                    log.warn("Resolver {} failed for '{}': {}", resolver.getSource(), query, e.getMessage());
                }
            }
        }

        // DB results first (higher priority), then Google
        return new PageImpl<>(merged, pageable, merged.size());
    }

    private EnumLocationType mapLocationType(String type) {
        if (type == null) return EnumLocationType.POI;
        return switch (type) {
            case "AIRPORT" -> EnumLocationType.AIRPORT;
            case "STATION" -> EnumLocationType.STATION;
            case "CITY" -> EnumLocationType.CITY;
            default -> EnumLocationType.POI;
        };
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
