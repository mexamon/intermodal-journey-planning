package com.thy.cloud.service.api.datasync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for data synchronization operations.
 * Allows triggering Amadeus sync, GTFS import, etc. via API.
 */
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
@Slf4j
public class DataSyncController {

    private final List<DataSourceSyncService> syncServices;

    /**
     * Trigger a full sync for a specific data source.
     *
     * @param sourceType "AMADEUS" or "GTFS"
     */
    @PostMapping("/{sourceType}")
    public ResponseEntity<SyncResult> triggerSync(
            @PathVariable String sourceType,
            @RequestParam(defaultValue = "*") String region) {

        var service = syncServices.stream()
                .filter(s -> s.getSourceType().equalsIgnoreCase(sourceType))
                .findFirst()
                .orElse(null);

        if (service == null) {
            return ResponseEntity.badRequest().body(
                    SyncResult.builder(sourceType)
                            .errors(1).warn("Unknown source type: " + sourceType)
                            .build()
            );
        }

        SyncRequest request = SyncRequest.fullSyncForRegion(
                region,
                LocalDate.now(),
                LocalDate.now().plusMonths(6)
        );

        log.info("Starting {} sync for region '{}'", sourceType, region);
        SyncResult result = service.sync(request);
        log.info("{} sync complete: {}", sourceType, result);

        return ResponseEntity.ok(result);
    }

    /**
     * Get available sync source types.
     */
    @GetMapping("/sources")
    public ResponseEntity<Map<String, Object>> listSources() {
        var sources = syncServices.stream()
                .map(s -> Map.of(
                        "type", (Object) s.getSourceType()
                ))
                .toList();
        return ResponseEntity.ok(Map.of("sources", sources));
    }
}
