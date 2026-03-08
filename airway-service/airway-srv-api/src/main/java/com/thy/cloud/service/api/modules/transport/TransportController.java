package com.thy.cloud.service.api.modules.transport;

import com.thy.cloud.base.core.api.Result;
import com.thy.cloud.service.api.modules.transport.model.EdgeSearchRequest;
import com.thy.cloud.service.api.modules.transport.model.FareRequest;
import com.thy.cloud.service.api.modules.transport.service.TransportService;
import com.thy.cloud.service.dao.entity.transport.Fare;
import com.thy.cloud.service.dao.entity.transport.EdgeTrip;
import com.thy.cloud.service.dao.entity.transport.TransportMode;
import com.thy.cloud.service.dao.entity.transport.TransportServiceArea;
import com.thy.cloud.service.dao.entity.transport.TransportStop;
import com.thy.cloud.service.dao.entity.transport.TransportationEdge;
import com.thy.cloud.service.dao.enums.EnumFareClass;
import com.thy.cloud.service.dao.enums.EnumPricingType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transport")
@RequiredArgsConstructor
public class TransportController {

    private final TransportService transportService;

    // ── Transport Mode ────────────────────────────────────────

    @GetMapping("/modes")
    public Result<List<TransportMode>> listModes(
            @RequestParam(required = false, defaultValue = "false") boolean all) {
        return Result.success(all ? transportService.listAllModes() : transportService.listActiveModes());
    }

    @GetMapping("/modes/{id}")
    public Result<TransportMode> getMode(@PathVariable UUID id) {
        return Result.success(transportService.getMode(id));
    }

    @GetMapping("/modes/code/{code}")
    public Result<TransportMode> getModeByCode(@PathVariable String code) {
        return Result.success(transportService.getModeByCode(code));
    }

    @PutMapping(value = "/modes/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<TransportMode> updateMode(@PathVariable UUID id,
                                            @RequestBody TransportMode mode) {
        TransportMode existing = transportService.getMode(id);
        if (mode.getName() != null) existing.setName(mode.getName());
        if (mode.getMaxWalkingAccessM() != null) existing.setMaxWalkingAccessM(mode.getMaxWalkingAccessM());
        if (mode.getDefaultSpeedKmh() != null) existing.setDefaultSpeedKmh(mode.getDefaultSpeedKmh());
        if (mode.getIsActive() != null) existing.setIsActive(mode.getIsActive());
        if (mode.getIcon() != null) existing.setIcon(mode.getIcon());
        if (mode.getColorHex() != null) existing.setColorHex(mode.getColorHex());
        if (mode.getSortOrder() != null) existing.setSortOrder(mode.getSortOrder());
        return Result.success(transportService.saveMode(existing));
    }

    // ── Service Area ──────────────────────────────────────────

    @GetMapping("/service-areas")
    public Result<List<TransportServiceArea>> listServiceAreas(
            @RequestParam(required = false) UUID modeId) {
        return Result.success(transportService.listServiceAreas(modeId));
    }

    @GetMapping("/service-areas/{id}")
    public Result<TransportServiceArea> getServiceArea(@PathVariable UUID id) {
        return Result.success(transportService.getServiceArea(id));
    }

    @PostMapping(value = "/service-areas", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<TransportServiceArea> createServiceArea(
            @RequestBody TransportServiceArea area) {
        return Result.success(transportService.saveServiceArea(area));
    }

    @PutMapping(value = "/service-areas/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<TransportServiceArea> updateServiceArea(@PathVariable UUID id,
                                                          @RequestBody TransportServiceArea area) {
        TransportServiceArea existing = transportService.getServiceArea(id);
        if (area.getName() != null) existing.setName(area.getName());
        if (area.getAreaType() != null) existing.setAreaType(area.getAreaType());
        if (area.getCenterLat() != null) existing.setCenterLat(area.getCenterLat());
        if (area.getCenterLon() != null) existing.setCenterLon(area.getCenterLon());
        if (area.getRadiusM() != null) existing.setRadiusM(area.getRadiusM());
        if (area.getCountryIsoCode() != null) existing.setCountryIsoCode(area.getCountryIsoCode());
        if (area.getCity() != null) existing.setCity(area.getCity());
        if (area.getIsActive() != null) existing.setIsActive(area.getIsActive());
        if (area.getConfigJson() != null) existing.setConfigJson(area.getConfigJson());
        return Result.success(transportService.saveServiceArea(existing));
    }

    @DeleteMapping("/service-areas/{id}")
    public Result<Void> deleteServiceArea(@PathVariable UUID id) {
        transportService.deleteServiceArea(id);
        return Result.success(null);
    }

    // ── Stops ─────────────────────────────────────────────────

    @GetMapping("/stops")
    public Result<List<TransportStop>> listStops(@RequestParam UUID serviceAreaId) {
        return Result.success(transportService.listStopsByServiceArea(serviceAreaId));
    }

    // ── Edges ─────────────────────────────────────────────────

    @GetMapping("/edges")
    public Result<List<TransportationEdge>> listEdges() {
        return Result.success(transportService.listAllEdges());
    }

    @PostMapping(value = "/edges/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Page<TransportationEdge>> searchEdges(@RequestBody @Valid EdgeSearchRequest request,
            Pageable pageable) {
        return Result.success(transportService.searchEdges(request, pageable));
    }

    @GetMapping("/edges/from/{originId}")
    public Result<List<TransportationEdge>> getEdgesFromOrigin(@PathVariable UUID originId) {
        return Result.success(transportService.getEdgesFromOrigin(originId));
    }

    // ── Fares ─────────────────────────────────────────────────

    @GetMapping("/fares")
    public Result<List<Fare>> listFares(@RequestParam(required = false) UUID edgeId) {
        if (edgeId != null) {
            return Result.success(transportService.listFaresByEdge(edgeId));
        }
        return Result.success(transportService.listAllFares());
    }

    @GetMapping("/fares/{id}")
    public Result<Fare> getFare(@PathVariable UUID id) {
        return Result.success(transportService.getFare(id));
    }

    @PostMapping(value = "/fares", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Fare> createFare(@RequestBody @Valid FareRequest request) {
        Fare fare = mapToFareEntity(request, new Fare());
        return Result.success(transportService.saveFare(fare));
    }

    @PutMapping(value = "/fares/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Fare> updateFare(@PathVariable UUID id,
                                   @RequestBody @Valid FareRequest request) {
        request.setId(id); // for UniqueFareValidator self-check
        Fare existing = transportService.getFare(id);
        mapToFareEntity(request, existing);
        return Result.success(transportService.saveFare(existing));
    }

    @DeleteMapping("/fares/{id}")
    public Result<Void> deleteFare(@PathVariable UUID id) {
        transportService.deleteFare(id);
        return Result.success(null);
    }

    private Fare mapToFareEntity(FareRequest request, Fare fare) {
        TransportationEdge edge = new TransportationEdge();
        edge.setId(request.getEdgeId());
        fare.setEdge(edge);

        if (request.getTripId() != null) {
            EdgeTrip trip = new EdgeTrip();
            trip.setId(request.getTripId());
            fare.setTrip(trip);
        } else {
            fare.setTrip(null);
        }

        fare.setFareClass(EnumFareClass.valueOf(request.getFareClass()));
        fare.setPricingType(EnumPricingType.valueOf(request.getPricingType()));
        fare.setPriceCents(request.getPriceCents());
        fare.setCurrency(request.getCurrency());
        fare.setRefundable(request.getRefundable() != null ? request.getRefundable() : false);
        fare.setChangeable(request.getChangeable() != null ? request.getChangeable() : false);
        fare.setLuggageKg(request.getLuggageKg());
        fare.setCabinLuggageKg(request.getCabinLuggageKg());
        return fare;
    }
}

