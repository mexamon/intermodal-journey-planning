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

        // Origin: either locationId or iataCode must be provided
        boolean hasOrigin = (request.getOriginLocationId() != null)
                || (request.getOriginIataCode() != null && !request.getOriginIataCode().isBlank());
        if (!hasOrigin) {
            context.buildConstraintViolationWithTemplate("Kalkış noktası belirtilmelidir (lokasyon ID veya IATA kodu)")
                    .addPropertyNode("originIataCode")
                    .addConstraintViolation();
            valid = false;
        }

        // Destination: either locationId or iataCode must be provided
        boolean hasDest = (request.getDestinationLocationId() != null)
                || (request.getDestinationIataCode() != null && !request.getDestinationIataCode().isBlank());
        if (!hasDest) {
            context.buildConstraintViolationWithTemplate("Varış noktası belirtilmelidir (lokasyon ID veya IATA kodu)")
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
                context.buildConstraintViolationWithTemplate("Kalkış ve varış noktası aynı olamaz")
                        .addConstraintViolation();
                valid = false;
            }
        }

        // Max transfers limit
        if (request.getMaxTransfers() < 0 || request.getMaxTransfers() > 6) {
            context.buildConstraintViolationWithTemplate("Maksimum aktarma sayısı 0-6 arasında olmalıdır")
                    .addPropertyNode("maxTransfers")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
