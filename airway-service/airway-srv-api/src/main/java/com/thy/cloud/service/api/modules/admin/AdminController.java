package com.thy.cloud.service.api.modules.admin;

import com.thy.cloud.base.core.api.Result;
import com.thy.cloud.service.api.modules.admin.service.AdminService;
import com.thy.cloud.service.dao.entity.auth.AppUser;
import com.thy.cloud.service.dao.entity.reference.RefCountry;
import com.thy.cloud.service.dao.entity.reference.RefRegion;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    // ── User ──────────────────────────────────────────────────

    @GetMapping("/users/{id}")
    public Result<AppUser> getUser(@PathVariable UUID id) {
        return Result.success(adminService.getUser(id));
    }

    @GetMapping("/users/email/{email}")
    public Result<AppUser> getUserByEmail(@PathVariable String email) {
        return Result.success(adminService.getUserByEmail(email));
    }

    // ── Reference Data ────────────────────────────────────────

    @GetMapping("/ref/countries")
    public Result<List<RefCountry>> listCountries() {
        return Result.success(adminService.listCountries());
    }

    @GetMapping("/ref/countries/{isoCode}")
    public Result<RefCountry> getCountry(@PathVariable String isoCode) {
        return Result.success(adminService.getCountryByIsoCode(isoCode));
    }

    @GetMapping("/ref/regions")
    public Result<List<RefRegion>> listRegions() {
        return Result.success(adminService.listRegions());
    }
}
