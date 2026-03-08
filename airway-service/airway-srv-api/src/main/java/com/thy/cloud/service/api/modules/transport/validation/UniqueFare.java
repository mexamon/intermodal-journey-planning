package com.thy.cloud.service.api.modules.transport.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom constraint: ensures no duplicate fare exists for the same
 * edge + trip + fareClass combination in the database.
 */
@Documented
@Constraint(validatedBy = UniqueFareValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueFare {

    String message() default "A fare with this edge, trip and fare class combination already exists";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
