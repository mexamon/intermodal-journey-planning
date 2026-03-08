package com.thy.cloud.service.api.modules.transport;

import com.thy.cloud.base.core.api.Result;
import com.thy.cloud.service.api.modules.transport.model.EdgeSearchRequest;
import com.thy.cloud.service.api.modules.transport.service.TransportService;
import com.thy.cloud.service.dao.entity.transport.TransportMode;
import com.thy.cloud.service.dao.entity.transport.TransportServiceArea;
import com.thy.cloud.service.dao.entity.transport.TransportStop;
import com.thy.cloud.service.dao.entity.transport.TransportationEdge;
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

    @PostMapping(value = "/edges/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Page<TransportationEdge>> searchEdges(@RequestBody @Valid EdgeSearchRequest request,
            Pageable pageable) {
        return Result.success(transportService.searchEdges(request, pageable));
    }

    @GetMapping("/edges/from/{originId}")
    public Result<List<TransportationEdge>> getEdgesFromOrigin(@PathVariable UUID originId) {
        return Result.success(transportService.getEdgesFromOrigin(originId));
    }
}

