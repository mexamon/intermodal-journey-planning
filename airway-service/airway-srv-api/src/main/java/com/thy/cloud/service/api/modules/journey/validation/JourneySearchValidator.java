package com.thy.cloud.service.api.modules.journey.validation;

import com.thy.cloud.service.api.modules.journey.model.JourneySearchRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates JourneySearchRequest before it reaches the service layer.
 * Runs as a Bean Validation constraint — no need for manual checks in the service.
 */
public class JourneySearchValidator implements ConstraintValidator<JourneySearchValidation, JourneySearchRequest> {

    @Override
    public boolean isValid(JourneySearchRequest request, ConstraintValidatorContext context) {
        if (request == null) return false;

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        // Origin: locationId, iataCode, OR query text must be provided
        boolean hasOrigin = (request.getOriginLocationId() != null)
                || (request.getOriginIataCode() != null && !request.getOriginIataCode().isBlank())
                || (request.getOriginQuery() != null && !request.getOriginQuery().isBlank());
        if (!hasOrigin) {
            context.buildConstraintViolationWithTemplate("Origin must be specified (location ID, IATA code, or query text)")
                    .addPropertyNode("originIataCode")
                    .addConstraintViolation();
            valid = false;
        }

        // Destination: locationId, iataCode, OR query text must be provided
        boolean hasDest = (request.getDestinationLocationId() != null)
                || (request.getDestinationIataCode() != null && !request.getDestinationIataCode().isBlank())
                || (request.getDestinationQuery() != null && !request.getDestinationQuery().isBlank());
        if (!hasDest) {
            context.buildConstraintViolationWithTemplate("Destination must be specified (location ID, IATA code, or query text)")
                    .addPropertyNode("destinationIataCode")
                    .addConstraintViolation();
            valid = false;
        }

        // Same origin and destination check
        if (hasOrigin && hasDest) {
            boolean sameById = request.getOriginLocationId() != null
                    && request.getOriginLocationId().equals(request.getDestinationLocationId());
            boolean sameByIata = request.getOriginIataCode() != null
                    && request.getOriginIataCode().equalsIgnoreCase(request.getDestinationIataCode());
            if (sameById || sameByIata) {
                context.buildConstraintViolationWithTemplate("Origin and destination cannot be the same")
                        .addConstraintViolation();
                valid = false;
            }
        }

        // Max transfers limit
        if (request.getMaxTransfers() < 0 || request.getMaxTransfers() > 6) {
            context.buildConstraintViolationWithTemplate("Max transfers must be between 0 and 6")
                    .addPropertyNode("maxTransfers")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
