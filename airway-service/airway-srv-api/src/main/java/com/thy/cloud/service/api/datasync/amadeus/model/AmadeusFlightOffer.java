package com.thy.cloud.service.api.datasync.amadeus.model;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Model matching the Amadeus Flight Offers Search API response structure.
 * <p>
 * Real Amadeus API returns this format from {@code GET /v2/shopping/flight-offers}.
 * Our mock client returns the same structure for seamless swap later.
 */
public record AmadeusFlightOffer(
        String id,
        String source,
        List<Itinerary> itineraries,
        Price price,
        List<TravelerPricing> travelerPricings
) {
    public record Itinerary(
            String duration, // ISO-8601 e.g. "PT3H50M"
            List<Segment> segments
    ) {}

    public record Segment(
            Endpoint departure,
            Endpoint arrival,
            String carrierCode,       // "TK", "PC"
            String number,            // "1987"
            Aircraft aircraft,
            String duration,          // "PT3H50M"
            int numberOfStops,
            List<Integer> operatingDays // [1,2,3,4,5,6,7] = every day
    ) {}

    public record Endpoint(
            String iataCode,          // "SAW"
            String terminal,          // "1"
            LocalTime at              // 06:30
    ) {}

    public record Aircraft(
            String code              // "77W" (Boeing 777-300ER)
    ) {}

    public record Price(
            String currency,         // "EUR"
            int totalCents           // 18500 = €185.00
    ) {}

    public record TravelerPricing(
            String travelerId,
            List<FareDetail> fareDetailsBySegment
    ) {}

    public record FareDetail(
            String segmentId,
            String cabin,            // "ECONOMY", "BUSINESS"
            String fareBasis,        // "YOWTR"
            String brandedFare,      // "ECOFLY"
            String clazz             // "Y" (booking class)
    ) {}
}
