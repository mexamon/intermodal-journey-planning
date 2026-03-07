package com.thy.cloud.data.jpa.entity.auditor;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

public class AppAuditorAware implements AuditorAware<String> {


    /**
     * @see AuditorAware
     * @return Optional<String>
     */
    @Override
    public Optional<String> getCurrentAuditor() {

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        return Optional.of(authentication.getName());
    }

    /**
     * Optional
     * @see AuditorAware
     * @return String
     */
    public String getCurrentAuditorOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        if(authentication.getPrincipal() instanceof String) {
            return ((String) authentication.getPrincipal());
        }
        if(authentication.getPrincipal() instanceof User) {
            return ((User) authentication.getPrincipal()).getUsername();
        }
        return authentication.getPrincipal().toString();
    }

    /**
     * Optional
     * @see AuditorAware
     * @return String
     */
    public Optional<String> getCurrentAuditorOptionalTwo() {
        UserDetails user;
        try {
            user = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return Optional.ofNullable(user.getUsername());
        }catch (Exception e){
            return Optional.empty();
        }
    }
}