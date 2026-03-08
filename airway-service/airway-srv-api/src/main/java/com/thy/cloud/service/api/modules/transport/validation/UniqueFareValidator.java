package com.thy.cloud.service.api.modules.transport.validation;

import com.thy.cloud.service.api.modules.transport.model.FareRequest;
import com.thy.cloud.service.dao.enums.EnumFareClass;
import com.thy.cloud.service.dao.repository.transport.FareRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates that no duplicate fare exists for the same edge + trip + fareClass.
 * Spring-managed bean — FareRepository is injected by the container.
 */
@Component
@RequiredArgsConstructor
public class UniqueFareValidator implements ConstraintValidator<UniqueFare, FareRequest> {

    private final FareRepository fareRepository;

    @Override
    public boolean isValid(FareRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getEdgeId() == null || request.getFareClass() == null) {
            return true; // other validators handle null checks
        }

        EnumFareClass fareClass;
        try {
            fareClass = EnumFareClass.valueOf(request.getFareClass());
        } catch (IllegalArgumentException e) {
            return true; // invalid enum value — handled by other validation
        }

        boolean exists = fareRepository.existsByEdgeIdAndTripIdAndFareClass(
                request.getEdgeId(),
                request.getTripId(),
                fareClass
        );

        // Allow update: if editing an existing fare, skip duplicate check for itself
        if (exists && request.getId() != null) {
            exists = !fareRepository.isOwnedBy(
                    request.getId(),
                    request.getEdgeId(),
                    request.getTripId(),
                    fareClass
            );
        }

        if (exists) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "A fare with class '" + request.getFareClass() +
                    "' already exists for this edge" +
                    (request.getTripId() != null ? " and trip" : "")
            ).addPropertyNode("fareClass").addConstraintViolation();
            return false;
        }

        return true;
    }
}
