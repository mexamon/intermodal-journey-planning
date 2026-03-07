package com.thy.cloud.base.core.exception.validator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.thy.cloud.base.core.error.ErrorItem;
import com.thy.cloud.base.core.error.ErrorResponse;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSR303 Validator(Hibernate Validator) Tools.
 * ConstraintViolation contains propertyPath,
 * Message and invalidValue and other information. Provides various convert methods, suitable for different i18n needs:
 * 1. List<String>, String content is message 2. List<String>, String content is propertyPath + separator + message 3.
 * Map<propertyPath, message> See the wiki for details: https://github.com/springside/springside4/wiki/HibernateValidator
 *
 * @author Engin Mahmut
 *
 */
public class BeanValidators {

    private BeanValidators(){

    }

    /**
     * Call the validate method of JSR303, and throw ConstraintViolationException when validation fails.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void validateWithException(Validator validator, Object object, Class<?>... groups) throws ConstraintViolationException {
        Set constraintViolations = validator.validate(object, groups);
        if (!constraintViolations.isEmpty()) {
            throw new ConstraintViolationException(constraintViolations);
        }
    }

    /**
     * Helper method, convert List <message> in Set <ConstraintViolations> in ConstraintViolationException.
     */
    public static List<String> extractMessage(ConstraintViolationException e) {
        return extractMessage(e.getConstraintViolations());
    }

    /**
     * Helper method, convert Set <ConstraintViolation> to List <message>
     */
    @SuppressWarnings("rawtypes")
    public static List<String> extractMessage(Set<? extends ConstraintViolation> constraintViolations) {
        List<String> errorMessages = Lists.newArrayList();
        for (ConstraintViolation violation : constraintViolations) {
            errorMessages.add(violation.getMessage());
        }
        return errorMessages;
    }

    /**
     * Helper method, convert Set <ConstraintViolations> in ConstraintViolationException to Map <property, message>
     */
    public static Map<String, String> extractPropertyAndMessage(ConstraintViolationException e) {
        return extractPropertyAndMessage(e.getConstraintViolations());
    }

    /**
     * Auxiliary method, convert Set <ConstraintViolation> to Map <property, message>
     */
    @SuppressWarnings("rawtypes")
    public static Map<String, String> extractPropertyAndMessage(Set<? extends ConstraintViolation> constraintViolations) {
        Map<String, String> errorMessages = Maps.newHashMap();
        for (ConstraintViolation violation : constraintViolations) {
            errorMessages.put(violation.getPropertyPath().toString(), violation.getMessage());
        }
        return errorMessages;
    }

    /**
     * Helper method, convert Set <ConstraintViolations> in ConstraintViolationException to List <propertyPath message>
     */
    public static List<String> extractPropertyAndMessageAsList(ConstraintViolationException e) {
        return extractPropertyAndMessageAsList(e.getConstraintViolations(), " ");
    }

    /**
     * As a helper method, convert Set <ConstraintViolations> to List <propertyPath message>
     */
    @SuppressWarnings("rawtypes")
    public static List<String> extractPropertyAndMessageAsList(Set<? extends ConstraintViolation> constraintViolations) {
        return extractPropertyAndMessageAsList(constraintViolations, " ");
    }

    /**
     * Helper method, convert Set <ConstraintViolations> in ConstraintViolationException to List <propertyPath + separator + message>
     */
    public static List<String> extractPropertyAndMessageAsList(ConstraintViolationException e, String separator) {
        return extractPropertyAndMessageAsList(e.getConstraintViolations(), separator);
    }

    /**
     * Auxiliary method, convert Set <ConstraintViolation> to List <propertyPath + separator + message>
     */
    @SuppressWarnings("rawtypes")
    public static List<String> extractPropertyAndMessageAsList(Set<? extends ConstraintViolation> constraintViolations, String separator) {
        List<String> errorMessages = Lists.newArrayList();
        for (ConstraintViolation violation : constraintViolations) {
            //violation.getPropertyPath() + separator +
            errorMessages.add(violation.getMessage());
        }
        return errorMessages;
    }

    public static List<ObjectError> extractPropertyAndMessage(MethodArgumentNotValidException methodArgumentNotValidException) {
        return methodArgumentNotValidException.getBindingResult().getAllErrors();
    }

    public static List<String> extractPropertyAndMessageAsList(MethodArgumentNotValidException methodArgumentNotValidException) {

        //Using in Global Exception Handler Class
        //List<String> messages = BeanValidators.extractPropertyAndMessageAsList(ex)
        //return ResponseResult.fail(ResultType.VALIDATE_FAILED, messages)
        List<String> errorMessages = Lists.newArrayList();
        List<ObjectError> allErrors = extractPropertyAndMessage(methodArgumentNotValidException);
        for (ObjectError violation : allErrors) {
            //violation.getCodes()[0] + separator +
            String message = (violation instanceof FieldError ? ((FieldError) violation).getField() + " " : "") + violation.getDefaultMessage();
            errorMessages.add(message + ";");
        }
        return errorMessages;
    }

    public static ErrorResponse extractPropertyAndMessageAsErrorList(MethodArgumentNotValidException methodArgumentNotValidException) {

        //Using in Global Exception Handler Class
        //ErrorResponse messages = BeanValidators.extractPropertyAndMessageAsErrorList(ex)
        //return ResponseResult.fail(ResultType.VALIDATE_FAILED, messages)
        ErrorResponse errorMessages = new ErrorResponse();

        methodArgumentNotValidException.getBindingResult().getFieldErrors().forEach(error -> {
            ErrorItem errorItems = new ErrorItem();
            errorItems.setCode(error.getField());
            errorItems.setMessage(error.getDefaultMessage());
            errorMessages.addError(errorItems);
        });
        methodArgumentNotValidException.getBindingResult().getGlobalErrors().forEach(error -> {
            ErrorItem errorItems = new ErrorItem();
            errorItems.setCode(error.getObjectName());
            errorItems.setMessage(error.getDefaultMessage());
            errorMessages.addError(errorItems);
        });
        return errorMessages;
    }


}