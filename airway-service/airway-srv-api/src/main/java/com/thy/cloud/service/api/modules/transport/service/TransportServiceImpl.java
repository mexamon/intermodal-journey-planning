package com.thy.cloud.service.api.modules.transport.service;

import com.thy.cloud.service.api.modules.transport.model.EdgeSearchRequest;
import com.thy.cloud.service.api.modules.transport.specs.EdgeSpecs;
import com.thy.cloud.service.dao.entity.transport.Fare;
import com.thy.cloud.service.dao.entity.transport.EdgeTrip;
import com.thy.cloud.service.dao.entity.transport.TransportMode;
import com.thy.cloud.service.dao.entity.transport.TransportServiceArea;
import com.thy.cloud.service.dao.entity.transport.TransportStop;
import com.thy.cloud.service.dao.entity.transport.TransportationEdge;
import com.thy.cloud.service.dao.repository.inventory.LocationRepository;
import com.thy.cloud.service.dao.repository.inventory.ProviderRepository;
import com.thy.cloud.service.dao.repository.transport.FareRepository;
import com.thy.cloud.service.dao.repository.transport.TransportModeRepository;
import com.thy.cloud.service.dao.repository.transport.TransportServiceAreaRepository;
import com.thy.cloud.service.dao.repository.transport.TransportStopRepository;
import com.thy.cloud.service.dao.repository.transport.TransportationEdgeRepository;
import com.thy.cloud.service.dao.repository.transport.EdgeTripRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransportServiceImpl implements TransportService {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final TransportModeRepository transportModeRepository;
    private final TransportServiceAreaRepository serviceAreaRepository;
    private final TransportStopRepository transportStopRepository;
    private final TransportationEdgeRepository edgeRepository;
    private final EdgeTripRepository edgeTripRepository;
    private final ProviderRepository providerRepository;
    private final LocationRepository locationRepository;
    private final FareRepository fareRepository;

    // ── Mode ──────────────────────────────────────────────────

    @Override
    @Cacheable(value = "active_modes")
    public List<TransportMode> listActiveModes() {
        return transportModeRepository.findByIsActiveTrueOrderBySortOrderAsc();
    }

    @Override
    public List<TransportMode> listAllModes() {
        return transportModeRepository.findAll();
    }

    @Override
    public TransportMode getMode(UUID id) {
        return transportModeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transport mode not found: " + id));
    }

    @Override
    public TransportMode getModeByCode(String code) {
        return transportModeRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Transport mode not found for code: " + code));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "active_modes", allEntries = true),
            @CacheEvict(value = "diverse_modes", allEntries = true)
    })
    public TransportMode saveMode(TransportMode mode) {
        validateConfigJson(mode);
        return transportModeRepository.save(mode);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "active_modes", allEntries = true),
            @CacheEvict(value = "diverse_modes", allEntries = true)
    })
    public void deleteMode(UUID id) {
        TransportMode mode = getMode(id);
        if (edgeRepository.existsByTransportModeId(id)) {
            throw new IllegalStateException("Cannot delete transport mode '" + mode.getCode()
                    + "' — it is used by existing edges.");
        }
        mode.setDeleted(true);
        transportModeRepository.save(mode);
    }

    // ── Service Area ──────────────────────────────────────────

    @Override
    public List<TransportServiceArea> listServiceAreas(UUID modeId) {
        if (modeId != null) {
            return serviceAreaRepository.findByTransportModeId(modeId);
        }
        return serviceAreaRepository.findAll();
    }

    @Override
    public List<TransportServiceArea> listAllServiceAreas() {
        return serviceAreaRepository.findAll();
    }

    @Override
    public TransportServiceArea getServiceArea(UUID id) {
        return serviceAreaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service area not found: " + id));
    }

    @Override
    @Transactional
    public TransportServiceArea saveServiceArea(TransportServiceArea area) {
        if (area.getTransportMode() != null && area.getTransportMode().getId() != null) {
            area.setTransportMode(transportModeRepository.findById(area.getTransportMode().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Transport mode not found: " + area.getTransportMode().getId())));
        }
        if (area.getProvider() != null && area.getProvider().getId() != null) {
            area.setProvider(providerRepository.findById(area.getProvider().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Provider not found: " + area.getProvider().getId())));
        }
        return serviceAreaRepository.save(area);
    }

    @Override
    @Transactional
    public void deleteServiceArea(UUID id) {
        TransportServiceArea area = getServiceArea(id);
        area.setDeleted(true);
        serviceAreaRepository.save(area);
    }

    // ── Stops ─────────────────────────────────────────────────

    @Override
    public List<TransportStop> listStopsByServiceArea(UUID serviceAreaId) {
        return transportStopRepository.findByServiceAreaId(serviceAreaId);
    }

    // ── Edges ─────────────────────────────────────────────────

    @Override
    public Page<TransportationEdge> searchEdges(EdgeSearchRequest request, Pageable pageable) {
        return edgeRepository.findAll(EdgeSpecs.filter(request), pageable);
    }

    @Override
    public List<TransportationEdge> getEdgesFromOrigin(UUID originId) {
        return edgeRepository.findByOriginLocationId(originId);
    }

    @Override
    public List<TransportationEdge> listAllEdges() {
        return edgeRepository.findAllWithRelations();
    }

    @Override
    public TransportationEdge getEdge(UUID id) {
        return edgeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Edge not found: " + id));
    }

    @Override
    @Transactional
    public TransportationEdge saveEdge(TransportationEdge edge) {
        // Resolve managed FK references
        if (edge.getTransportMode() != null && edge.getTransportMode().getId() != null) {
            edge.setTransportMode(transportModeRepository.findById(edge.getTransportMode().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Transport mode not found: " + edge.getTransportMode().getId())));
        }
        if (edge.getProvider() != null && edge.getProvider().getId() != null) {
            edge.setProvider(providerRepository.findById(edge.getProvider().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Provider not found: " + edge.getProvider().getId())));
        }
        if (edge.getOriginLocation() != null && edge.getOriginLocation().getId() != null) {
            edge.setOriginLocation(locationRepository.findById(edge.getOriginLocation().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Origin location not found: " + edge.getOriginLocation().getId())));
        }
        if (edge.getDestinationLocation() != null && edge.getDestinationLocation().getId() != null) {
            edge.setDestinationLocation(locationRepository.findById(edge.getDestinationLocation().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Destination location not found: " + edge.getDestinationLocation().getId())));
        }
        return edgeRepository.save(edge);
    }

    @Override
    @Transactional
    public void deleteEdge(UUID id) {
        TransportationEdge edge = getEdge(id);
        edge.setDeleted(true);
        edgeRepository.save(edge);
    }

    // ── Trips ─────────────────────────────────────────────────

    @Override
    public EdgeTrip getTrip(UUID id) {
        return edgeTripRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trip not found: " + id));
    }

    @Override
    @Transactional
    public EdgeTrip saveTrip(UUID edgeId, EdgeTrip trip) {
        TransportationEdge edge = getEdge(edgeId);
        trip.setEdge(edge);
        return edgeTripRepository.save(trip);
    }

    @Override
    @Transactional
    public void deleteTrip(UUID tripId) {
        EdgeTrip trip = getTrip(tripId);
        trip.setDeleted(true);
        edgeTripRepository.save(trip);
    }

    // ── Fares ─────────────────────────────────────────────────

    @Override
    public List<Fare> listAllFares() {
        return fareRepository.findAllActive();
    }

    @Override
    public List<Fare> listFaresByEdge(UUID edgeId) {
        return fareRepository.findByEdgeId(edgeId);
    }

    @Override
    public Fare getFare(UUID id) {
        return fareRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fare not found: " + id));
    }

    @Override
    @Transactional
    public Fare saveFare(Fare fare) {
        // Resolve managed FK references
        if (fare.getEdge() != null && fare.getEdge().getId() != null) {
            fare.setEdge(edgeRepository.findById(fare.getEdge().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Edge not found: " + fare.getEdge().getId())));
        }
        return fareRepository.save(fare);
    }

    @Override
    @Transactional
    public void deleteFare(UUID id) {
        Fare fare = getFare(id);
        fare.setDeleted(true);
        fareRepository.save(fare);
    }

    // ── Validation ───────────────────────────────────────────

    /**
     * Ensures config_json contains required fields for the journey algorithm.
     * <ul>
     *   <li>transfer_time_min — required, must be a non-negative integer</li>
     *   <li>category — required on the mode entity</li>
     * </ul>
     */
    private void validateConfigJson(TransportMode mode) {
        if (mode.getCategory() == null) {
            throw new IllegalArgumentException(
                    "Transport mode '" + mode.getCode() + "' must have a category.");
        }

        String configJson = mode.getConfigJson();
        if (configJson == null || configJson.isBlank()) {
            throw new IllegalArgumentException(
                    "Transport mode '" + mode.getCode() + "' must have config_json with 'transfer_time_min'.");
        }

        try {
            JsonNode root = JSON.readTree(configJson);
            JsonNode ttNode = root.get("transfer_time_min");
            if (ttNode == null || !ttNode.isNumber()) {
                throw new IllegalArgumentException(
                        "Transport mode '" + mode.getCode()
                                + "': config_json must contain 'transfer_time_min' as a number.");
            }
            if (ttNode.intValue() < 0) {
                throw new IllegalArgumentException(
                        "Transport mode '" + mode.getCode()
                                + "': 'transfer_time_min' cannot be negative.");
            }
        } catch (IllegalArgumentException e) {
            throw e; // re-throw our own validation errors
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Transport mode '" + mode.getCode()
                            + "': invalid config_json — " + e.getMessage());
        }
    }
}

