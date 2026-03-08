package com.thy.cloud.service.api.modules.inventory;

import com.thy.cloud.base.core.api.Result;
import com.thy.cloud.service.api.modules.inventory.model.LocationSearchRequest;
import com.thy.cloud.service.api.modules.inventory.model.ProviderRequest;
import com.thy.cloud.service.api.modules.inventory.service.InventoryService;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.entity.inventory.AirportProfile;
import com.thy.cloud.service.dao.entity.inventory.Provider;
import com.thy.cloud.service.dao.enums.EnumProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // ── Location ──────────────────────────────────────────────

    @PostMapping(value = "/locations/search", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Result<Page<Location>> searchLocations(@RequestBody @Valid LocationSearchRequest request,
            Pageable pageable) {
        return Result.success(inventoryService.searchLocations(request, pageable));
    }

    @GetMapping("/locations/{id}")
    public Result<Location> getLocation(@PathVariable UUID id) {
        return Result.success(inventoryService.getLocation(id));
    }

    @GetMapping("/locations/iata/{iataCode}")
    public Result<Location> getLocationByIata(@PathVariable String iataCode) {
        return Result.success(inventoryService.getLocationByIata(iataCode));
    }

    // ── Airport ───────────────────────────────────────────────

    @GetMapping("/airports/{locationId}/profile")
    public Result<AirportProfile> getAirportProfile(@PathVariable UUID locationId) {
        return Result.success(inventoryService.getAirportProfile(locationId));
    }

    @GetMapping("/airports/scheduled")
    public Result<List<AirportProfile>> getScheduledAirports() {
        return Result.success(inventoryService.getScheduledAirports());
    }

    // ── Provider ──────────────────────────────────────────────

    @GetMapping("/providers")
    public Result<List<Provider>> listProviders() {
        return Result.success(inventoryService.listProviders());
    }

    @GetMapping("/providers/{id}")
    public Result<Provider> getProvider(@PathVariable UUID id) {
        return Result.success(inventoryService.getProvider(id));
    }

    @GetMapping("/providers/code/{code}")
    public Result<Provider> getProviderByCode(@PathVariable String code) {
        return Result.success(inventoryService.getProviderByCode(code));
    }

    @PostMapping("/providers")
    public Result<Provider> createProvider(@RequestBody @Valid ProviderRequest request) {
        Provider provider = mapToEntity(request, new Provider());
        return Result.success(inventoryService.saveProvider(provider));
    }

    @PutMapping("/providers/{id}")
    public Result<Provider> updateProvider(@PathVariable UUID id,
                                           @RequestBody @Valid ProviderRequest request) {
        Provider existing = inventoryService.getProvider(id);
        mapToEntity(request, existing);
        return Result.success(inventoryService.saveProvider(existing));
    }

    @DeleteMapping("/providers/{id}")
    public Result<Void> deleteProvider(@PathVariable UUID id) {
        inventoryService.deleteProvider(id);
        return Result.success();
    }

    // ── Helper ────────────────────────────────────────────────

    private Provider mapToEntity(ProviderRequest request, Provider entity) {
        entity.setCode(request.getCode().toUpperCase().trim());
        entity.setName(request.getName().trim());
        entity.setType(EnumProviderType.valueOf(request.getType()));
        entity.setCountryIsoCode(request.getCountryIsoCode());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        return entity;
    }
}
