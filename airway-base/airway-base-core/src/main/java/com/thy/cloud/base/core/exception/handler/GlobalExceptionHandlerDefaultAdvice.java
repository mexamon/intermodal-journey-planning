package com.thy.cloud.base.core.exception.handler;

import com.thy.cloud.base.core.api.Result;
import com.thy.cloud.base.core.api.ResultType;
import com.thy.cloud.base.core.error.ErrorItem;
import com.thy.cloud.base.core.error.ErrorResponse;
import com.thy.cloud.base.core.exception.AccessDeniedException;
import com.thy.cloud.base.core.exception.ResourceNotFoundException;
import com.thy.cloud.base.core.exception.AirwayException;
import com.thy.cloud.base.core.exception.validator.BeanValidators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.Objects;

/**
 * @author Engin Mahmut
 *         Default Global Exception Handler Class
 * @version 1.1
 * @apiNote Revised for Exception Types and Validation Infrastructure
 */

public class GlobalExceptionHandlerDefaultAdvice {

    public static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandlerDefaultAdvice.class);

    public static String getMessage(Exception ex) {
        logger.error("Exception", ex);
        return ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage();
    }

    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(value = { AirwayException.class })
    public Result<?> airwayException(AirwayException ex) {
        logger.error("base exception:{}", ex.getMessage());
        return Result.fail(ex.getResultType());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = { HttpMessageNotReadableException.class, IOException.class })
    public Result<?> systemException(Exception ex) {
        logger.error("base exception:{}", ex.getMessage());
        return Result.fail(ResultType.JSON_PARSE_ERROR, getMessage(ex));
    }

    @ExceptionHandler(value = { MissingServletRequestParameterException.class })
    public Result<?> missingServletRequestParameterException(MissingServletRequestParameterException ex) {
        logger.error("missing servlet request parameter exception:{}", getMessage(ex));
        return Result.fail(ResultType.ARGUMENT_NOT_VALID);
    }

    @ExceptionHandler(value = { MultipartException.class })
    public Result<?> uploadFileLimitException(MultipartException ex) {
        logger.error("upload file failed:{}", getMessage(ex));
        return Result.fail(ResultType.UPLOAD_FAILED);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        logger.error(e.getMessage(), e);
        return Result.fail(ResultType.UPLOAD_FILE_SIZE_LIMIT);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = { MethodArgumentNotValidException.class })
    public Result<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        logger.error("service exception:{}", getMessage(ex));

        ErrorResponse messages = BeanValidators.extractPropertyAndMessageAsErrorList(ex);

        return Result.fail(ResultType.VALIDATE_FAILED, messages);

        /*
         * List<String> errors = new ArrayList<>();
         * for (FieldError error : ex.getBindingResult().getFieldErrors()) {
         * errors.add(error.getField() + ": " + error.getDefaultMessage());
         * }
         * for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
         * errors.add(error.getObjectName() + ": " + error.getDefaultMessage());
         * }
         * return ResponseResult.fail(ResultType.VALIDATE_FAILED,
         * ex.getBindingResult().getFieldErrors());
         */
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = {
            MethodArgumentTypeMismatchException.class, ServletRequestBindingException.class })
    public Result<?> badRequestExceptionHandler(Exception ex) {
        logger.error("Bad Request:{}", ex.getMessage());
        return Result.fail(ResultType.BAD_REQUEST, getMessage(ex));
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(value = { HttpRequestMethodNotSupportedException.class })
    public Result<?> methodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        logger.error("method not allowed:{}", getMessage(ex));

        var exceptionBuilder = new StringBuilder();
        exceptionBuilder.append(ex.getMethod());
        exceptionBuilder.append(
                " method is not supported for this request. Supported methods are ");

        Objects.requireNonNull(ex.getSupportedHttpMethods()).forEach(t -> exceptionBuilder.append(t).append(" "));

        return Result.fail(ResultType.METHOD_NOT_ALLOWED, exceptionBuilder);
    }

    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(value = { HttpMediaTypeNotSupportedException.class })
    public Result<?> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {

        logger.error("method not allowed:{}", getMessage(ex));

        var exceptionBuilder = new StringBuilder();
        exceptionBuilder.append(ex.getContentType());
        exceptionBuilder.append(" media type is not supported. Supported media types are ");
        ex.getSupportedMediaTypes().forEach(t -> exceptionBuilder.append(t).append(", "));

        return Result.fail(ResultType.UNSUPPORTED_MEDIA_TYPE,
                exceptionBuilder.substring(0, exceptionBuilder.length() - 2));
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = { NoHandlerFoundException.class, ResourceNotFoundException.class })
    public Result<?> resourceNotFoundExceptionHandler(Exception ex) {
        logger.error("Not Found:{}", getMessage(ex));
        return Result.fail(ResultType.NOT_FOUND, getMessage(ex));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = { EmptyResultDataAccessException.class,
            HttpMessageNotWritableException.class })
    public Result<?> emptyResultDataAccessExceptionHandler(Exception ex) {
        logger.error("Entity Does Not Exist:{}", ex.getMessage());
        return Result.fail(ResultType.NO_DATA, getMessage(ex));
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(value = { DuplicateKeyException.class })
    public Result<?> duplicateKeyException(DuplicateKeyException ex) {
        logger.error("primary key duplication exception:{}", getMessage(ex));
        return Result.fail(ResultType.DUPLICATE_PRIMARY_KEY);
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(value = { DataIntegrityViolationException.class })
    public Result<?> dataIntegrityExceptionHandler(Exception ex) {
        logger.error("data integrity violation exception:{}", getMessage(ex));
        return Result.fail(ResultType.DATA_INTEGRITY_VIOLATION, getMessage(ex));
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(value = { AccessDeniedException.class })
    public Result<?> handleAccessDeniedException(Exception ex) {
        logger.error("data integrity violation exception:{}", getMessage(ex));
        return Result.fail(ResultType.ACCESS_DENIED, getMessage(ex));
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(value = { ConstraintViolationException.class })
    public Result<?> constraintViolationExceptionHandler(ConstraintViolationException ex) {

        logger.error("constraint violation exception:{}", getMessage(ex));

        ErrorResponse errors = new ErrorResponse();
        for (@SuppressWarnings("rawtypes")
        ConstraintViolation violation : ex.getConstraintViolations()) {
            ErrorItem error = new ErrorItem();
            error.setCode(violation.getMessageTemplate());
            error.setMessage(violation.getMessage());
            errors.addError(error);
        }

        return Result.fail(ResultType.CONSTRAINT_VIOLATION, errors);
    }

}