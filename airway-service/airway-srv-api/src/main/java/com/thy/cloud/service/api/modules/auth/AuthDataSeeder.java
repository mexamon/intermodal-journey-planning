package com.thy.cloud.service.api.modules.auth;

import com.thy.cloud.service.dao.entity.auth.AppUser;
import com.thy.cloud.service.dao.enums.EnumUserRole;
import com.thy.cloud.service.dao.repository.auth.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Ensures admin@thy.com and agency@thy.com users exist with correct BCrypt hashes.
 * Runs on every startup to fix any hash mismatches from SQL migration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthDataSeeder implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        ensureUser("admin@thy.com", "admin", EnumUserRole.ADMIN, "System Admin");
        ensureUser("agency@thy.com", "agency", EnumUserRole.AGENCY, "Agency User");
    }

    private void ensureUser(String email, String rawPassword, EnumUserRole role, String displayName) {
        var existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            AppUser user = existing.get();
            // Fix hash if it doesn't match
            if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                userRepository.save(user);
                log.info("Updated password hash for user: {}", email);
            }
        } else {
            AppUser user = new AppUser();
            user.setId(UUID.randomUUID());
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setDisplayName(displayName);
            user.setIsActive(true);
            userRepository.save(user);
            log.info("Created user: {} with role {}", email, role.getValue());
        }
    }
}
