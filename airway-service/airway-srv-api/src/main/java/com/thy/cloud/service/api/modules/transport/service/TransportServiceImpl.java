package com.thy.cloud.service.api.modules.transport.service;

import com.thy.cloud.service.api.modules.transport.model.EdgeSearchRequest;
import com.thy.cloud.service.api.modules.transport.specs.EdgeSpecs;
import com.thy.cloud.service.dao.entity.transport.Fare;
import com.thy.cloud.service.dao.entity.transport.TransportMode;
import com.thy.cloud.service.dao.entity.transport.TransportServiceArea;
import com.thy.cloud.service.dao.entity.transport.TransportStop;
import com.thy.cloud.service.dao.entity.transport.TransportationEdge;
import com.thy.cloud.service.dao.repository.inventory.ProviderRepository;
import com.thy.cloud.service.dao.repository.transport.FareRepository;
import com.thy.cloud.service.dao.repository.transport.TransportModeRepository;
import com.thy.cloud.service.dao.repository.transport.TransportServiceAreaRepository;
import com.thy.cloud.service.dao.repository.transport.TransportStopRepository;
import com.thy.cloud.service.dao.repository.transport.TransportationEdgeRepository;
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
public class TransportServiceImpl implements TransportService {

    private final TransportModeRepository transportModeRepository;
    private final TransportServiceAreaRepository serviceAreaRepository;
    private final TransportStopRepository transportStopRepository;
    private final TransportationEdgeRepository edgeRepository;
    private final ProviderRepository providerRepository;
    private final FareRepository fareRepository;

    // ── Mode ──────────────────────────────────────────────────

    @Override
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
    public TransportMode saveMode(TransportMode mode) {
        return transportModeRepository.save(mode);
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
}

