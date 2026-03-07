package com.thy.cloud.service.api.modules.admin.service;

import com.thy.cloud.service.dao.entity.auth.AppUser;
import com.thy.cloud.service.dao.entity.reference.RefCountry;
import com.thy.cloud.service.dao.entity.reference.RefRegion;

import java.util.List;
import java.util.UUID;

public interface AdminService {

    // User
    AppUser getUser(UUID id);

    AppUser getUserByEmail(String email);

    // Reference
    List<RefCountry> listCountries();

    RefCountry getCountryByIsoCode(String isoCode);

    List<RefRegion> listRegions();
}
