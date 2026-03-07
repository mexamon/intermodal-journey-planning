package com.thy.cloud.service.api.resolver.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

/**
 * Context passed to EdgeResolver implementations, containing search parameters.
 */
public record EdgeSearchContext(
        LocalDate travelDate,
        LocalTime preferredTime,
        int maxResults,
        Set<String> allowedModes     // FLIGHT, BUS, TRAIN...
) {
    public static EdgeSearchContext of(LocalDate date) {
        return new EdgeSearchContext(date, null, 50, Set.of());
    }

    /**
     * Check if a mode is allowed (empty set = all modes allowed).
     */
    public boolean isModeAllowed(String modeCode) {
        return allowedModes == null || allowedModes.isEmpty() || allowedModes.contains(modeCode);
    }
}
