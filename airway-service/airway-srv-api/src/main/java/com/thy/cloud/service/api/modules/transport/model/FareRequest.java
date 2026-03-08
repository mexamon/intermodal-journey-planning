package com.thy.cloud.service.api.modules.transport.model;

import com.thy.cloud.service.api.modules.transport.validation.UniqueFare;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/**
 * Request DTO for creating/updating a Fare.
 * {@code @UniqueFare} ensures no duplicate edge+trip+fareClass in DB.
 */
@Data
@UniqueFare
public class FareRequest {

    /**
     * Set by controller on update — used by UniqueFareValidator to allow self-update.
     */
    private UUID id;

    @NotNull(message = "Edge ID is required")
    private UUID edgeId;

    private UUID tripId;

    @NotBlank(message = "Fare class is required")
    private String fareClass; // ECONOMY, PREMIUM_ECONOMY, BUSINESS, FIRST, STANDARD, COMFORT, VIP

    @NotBlank(message = "Pricing type is required")
    private String pricingType; // FIXED, ESTIMATED, DYNAMIC, FREE

    private Integer priceCents;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
    private String currency;

    private Boolean refundable = false;

    private Boolean changeable = false;

    private Integer luggageKg;

    private Integer cabinLuggageKg;
}
