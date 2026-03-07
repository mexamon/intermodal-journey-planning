package com.thy.cloud.service.dao.repository.auth;

import com.thy.cloud.data.jpa.repository.GenericRepository;
import com.thy.cloud.service.dao.entity.auth.AppUser;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends GenericRepository<AppUser, UUID> {

    Optional<AppUser> findByEmail(String email);
}
