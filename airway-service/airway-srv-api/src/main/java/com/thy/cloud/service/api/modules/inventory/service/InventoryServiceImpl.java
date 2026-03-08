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
import com.thy.cloud.service.dao.repository.transport.TransportServiceAreaRepository;
import com.thy.cloud.service.dao.repository.transport.TransportationEdgeRepository;
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
    private final TransportServiceAreaRepository serviceAreaRepository;
    private final TransportationEdgeRepository edgeRepository;
    private final List<LocationResolver> locationResolvers;

    // ── Location ──────────────────────────────────────────────

    @Override
    public Page<Location> searchLocations(LocationSearchRequest request, Pageable pageable) {
        Page<Location> dbResults = locationRepository.findAll(LocationSpecs.filter(request), pageable);
        List<Location> merged = new ArrayList<>(dbResults.getContent());

        String query = request.getName();
        if (query != null && !query.isBlank()) {
            for (LocationResolver resolver : locationResolvers) {
                if ("DB".equals(resolver.getSource())) continue;
                try {
                    List<ResolvedLocation> external = resolver.resolve(query, 5);
                    for (ResolvedLocation rl : external) {
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
                        loc.setSearchPriority(10);
                        merged.add(loc);
                    }
                } catch (Exception e) {
                    log.warn("Resolver {} failed for '{}': {}", resolver.getSource(), query, e.getMessage());
                }
            }
        }
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

    @Override
    @Transactional
    public Location saveLocation(Location location) {
        if (location.getId() == null && location.getIataCode() != null && !location.getIataCode().isBlank()) {
            locationRepository.findByIataCode(location.getIataCode()).ifPresent(existing -> {
                throw new IllegalStateException("Location with IATA '" + location.getIataCode() + "' already exists.");
            });
        }
        return locationRepository.save(location);
    }

    @Override
    @Transactional
    public void deleteLocation(UUID id) {
        Location existing = locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Location not found: " + id));

        boolean usedInEdges = edgeRepository.existsByOriginLocationIdOrDestinationLocationId(id, id);
        if (usedInEdges) {
            throw new IllegalStateException(
                    "Cannot delete location '" + existing.getName() + "': in use by transportation edges.");
        }
        locationRepository.deleteById(id);
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

    @Override
    public List<InventoryService.AirportLookupDto> lookupAirports(String query) {
        List<Location> airports;
        if (query != null && !query.isBlank()) {
            String q = "%" + query.trim().toLowerCase() + "%";
            airports = locationRepository.findAirportsByQuery(q);
        } else {
            airports = locationRepository.findAllAirports();
        }
        return airports.stream()
                .filter(l -> l.getIataCode() != null && !l.getIataCode().isBlank())
                .map(l -> new InventoryService.AirportLookupDto(
                        l.getIataCode(),
                        l.getName(),
                        l.getCity() != null ? l.getCity() : "",
                        l.getCountryIsoCode() != null ? l.getCountryIsoCode() : ""
                ))
                .toList();
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

    @Override
    @Transactional
    public Provider saveProvider(Provider provider) {
        // Check duplicate code on create
        if (provider.getId() == null) {
            providerRepository.findByCode(provider.getCode()).ifPresent(existing -> {
                throw new IllegalStateException("Provider with code '" + provider.getCode() + "' already exists.");
            });
        }
        return providerRepository.save(provider);
    }

    @Override
    @Transactional
    public void deleteProvider(UUID id) {
        Provider existing = providerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Provider not found: " + id));

        // Protection: check if provider is referenced in service areas or edges
        boolean usedInServiceAreas = serviceAreaRepository.existsByProviderId(id);
        boolean usedInEdges = edgeRepository.existsByProviderId(id);

        if (usedInServiceAreas || usedInEdges) {
            List<String> usages = new ArrayList<>();
            if (usedInServiceAreas) usages.add("service areas");
            if (usedInEdges) usages.add("transportation edges");
            throw new IllegalStateException(
                    "Cannot delete provider '" + existing.getName() + "': in use by " + String.join(" and ", usages) + "."
            );
        }

        providerRepository.deleteById(id);
    }
}
