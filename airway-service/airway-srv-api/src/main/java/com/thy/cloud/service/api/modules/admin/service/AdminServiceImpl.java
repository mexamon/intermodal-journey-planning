package com.thy.cloud.service.api.modules.admin.service;

import com.thy.cloud.service.dao.entity.auth.AppUser;
import com.thy.cloud.service.dao.entity.reference.RefCountry;
import com.thy.cloud.service.dao.entity.reference.RefRegion;
import com.thy.cloud.service.dao.repository.auth.AppUserRepository;
import com.thy.cloud.service.dao.repository.reference.RefCountryRepository;
import com.thy.cloud.service.dao.repository.reference.RefRegionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final AppUserRepository appUserRepository;
    private final RefCountryRepository refCountryRepository;
    private final RefRegionRepository refRegionRepository;

    // ── User ──────────────────────────────────────────────────

    @Override
    public AppUser getUser(UUID id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    @Override
    public AppUser getUserByEmail(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found for email: " + email));
    }

    // ── Reference ─────────────────────────────────────────────

    @Override
    public List<RefCountry> listCountries() {
        return refCountryRepository.findAll();
    }

    @Override
    public RefCountry getCountryByIsoCode(String isoCode) {
        return refCountryRepository.findByIsoCode(isoCode)
                .orElseThrow(() -> new EntityNotFoundException("Country not found: " + isoCode));
    }

    @Override
    public List<RefRegion> listRegions() {
        return refRegionRepository.findAll();
    }
}
