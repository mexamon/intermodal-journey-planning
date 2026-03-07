package com.thy.cloud.service.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for Google Maps Platform APIs (Places, Directions, Geocoding).
 */
@Configuration
public class GoogleApiConfig {

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${google.api.places.enabled:true}")
    private boolean placesEnabled;

    @Value("${google.api.directions.enabled:true}")
    private boolean directionsEnabled;

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
