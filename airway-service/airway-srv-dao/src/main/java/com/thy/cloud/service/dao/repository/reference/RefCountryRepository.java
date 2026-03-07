package com.thy.cloud.service.dao.repository.reference;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.reference.RefCountry;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefCountryRepository extends GenericRepository<RefCountry, UUID> {

    Optional<RefCountry> findByIsoCode(String isoCode);
}
