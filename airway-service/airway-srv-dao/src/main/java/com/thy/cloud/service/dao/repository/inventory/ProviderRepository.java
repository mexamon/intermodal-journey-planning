package com.thy.cloud.service.dao.repository.inventory;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.inventory.Provider;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends GenericRepository<Provider, UUID> {

    Optional<Provider> findByCode(String code);
}
