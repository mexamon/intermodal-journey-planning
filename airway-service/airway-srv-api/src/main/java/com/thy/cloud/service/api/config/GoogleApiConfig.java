package com.thy.cloud.service.api.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Google Maps Platform APIs (Places, Directions, Geocoding).
 */
@Configuration
public class GoogleApiConfig {

    private static final Logger log = LoggerFactory.getLogger(GoogleApiConfig.class);

    @Value("${google.api.key:}")
    private String apiKey;

    @Value("${google.api.places.enabled:true}")
    private boolean placesEnabled;

    @Value("${google.api.directions.enabled:true}")
    private boolean directionsEnabled;

    @PostConstruct
    void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("╔══════════════════════════════════════════════════════════════╗");
            log.warn("║  ⚠️  GOOGLE API KEY IS NOT CONFIGURED!                       ║");
            log.warn("║  Set 'google.api.key' in application.properties              ║");
            log.warn("║  or via env: GOOGLE_API_KEY=<your-key>                       ║");
            log.warn("║  Google Places & Directions features will NOT work.          ║");
            log.warn("╚══════════════════════════════════════════════════════════════╝");
        } else {
            log.info("✅ Google API key configured ({}...)", apiKey.substring(0, Math.min(8, apiKey.length())));
            log.info("   Places API: {}, Directions API: {}",
                    placesEnabled ? "ENABLED" : "DISABLED",
                    directionsEnabled ? "ENABLED" : "DISABLED");
        }
    }

    @Bean
    public RestTemplate googleRestTemplate() {
        return new RestTemplate();
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean isPlacesEnabled() {
        return placesEnabled;
    }

    public boolean isDirectionsEnabled() {
        return directionsEnabled;
    }
}
