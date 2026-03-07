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
    public Result<List<TransportMode>> listActiveModes() {
        return Result.success(transportService.listActiveModes());
    }

    @GetMapping("/modes/{id}")
    public Result<TransportMode> getMode(@PathVariable UUID id) {
        return Result.success(transportService.getMode(id));
    }

    @GetMapping("/modes/code/{code}")
    public Result<TransportMode> getModeByCode(@PathVariable String code) {
        return Result.success(transportService.getModeByCode(code));
    }

    // ── Service Area ──────────────────────────────────────────

    @GetMapping("/service-areas")
    public Result<List<TransportServiceArea>> listServiceAreas(@RequestParam(required = false) UUID modeId) {
        return Result.success(transportService.listServiceAreas(modeId));
    }

    @GetMapping("/service-areas/{id}")
    public Result<TransportServiceArea> getServiceArea(@PathVariable UUID id) {
        return Result.success(transportService.getServiceArea(id));
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
