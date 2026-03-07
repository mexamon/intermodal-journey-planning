package com.thy.cloud.service.api.datasync.amadeus;

import com.thy.cloud.service.api.datasync.amadeus.model.AmadeusFlightOffer;

import java.time.LocalDate;
import java.util.List;

/**
 * Client interface for the Amadeus Flight Offers Search API.
 * <p>
 * Implementations:
 * <ul>
 *     <li>{@link MockAmadeusClient} — hardcoded dummy data for development</li>
 *     <li>(future) {@code RealAmadeusClient} — calls real Amadeus API with OAuth2</li>
 * </ul>
 */
public interface AmadeusClient {

    /**
     * Search for flight offers between two airports.
     *
     * @param originIata      origin IATA code (e.g. "SAW")
     * @param destinationIata destination IATA code (e.g. "LHR")
     * @param departureDate   travel date
     * @param maxOffers       maximum number of offers to return
     * @return list of flight offers
     */
    List<AmadeusFlightOffer> searchFlights(String originIata, String destinationIata,
                                            LocalDate departureDate, int maxOffers);

    /**
     * Get all available routes (for full sync).
     *
     * @return all flight offers across all routes
     */
    List<AmadeusFlightOffer> getAllScheduledFlights();
}
