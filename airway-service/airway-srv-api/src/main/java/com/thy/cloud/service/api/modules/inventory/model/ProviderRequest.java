package com.thy.cloud.service.api.modules.inventory.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating/updating a Provider.
 */
@Data
public class ProviderRequest {

    @NotBlank(message = "Code is required")
    @Size(min = 2, max = 10, message = "Code must be 2-10 characters")
    private String code;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be 2-100 characters")
    private String name;

    @NotNull(message = "Type is required")
    private String type; // EnumProviderType value: AIRLINE, BUS_COMPANY, etc.

    @Size(min = 2, max = 2, message = "Country ISO code must be 2 characters")
    private String countryIsoCode;

    private Boolean isActive = true;
}
