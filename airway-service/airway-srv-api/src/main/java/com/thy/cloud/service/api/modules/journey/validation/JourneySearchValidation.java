package com.thy.cloud.service.api.modules.journey.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for JourneySearchRequest.
 * Ensures that origin and destination are properly specified
 * (either by locationId or iataCode) before reaching the service layer.
 */
@Documented
@Constraint(validatedBy = JourneySearchValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JourneySearchValidation {

    String message() default "Invalid journey search parameters";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
