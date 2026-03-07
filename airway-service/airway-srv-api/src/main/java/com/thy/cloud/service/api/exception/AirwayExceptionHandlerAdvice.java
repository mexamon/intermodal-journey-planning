package com.thy.cloud.service.api.exception;

import com.thy.cloud.base.core.api.Result;
import com.thy.cloud.base.core.api.ResultType;
import com.thy.cloud.base.core.exception.handler.GlobalExceptionHandlerDefaultAdvice;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class AirwayExceptionHandlerAdvice extends GlobalExceptionHandlerDefaultAdvice {

    private static final Logger logger = LoggerFactory.getLogger(AirwayExceptionHandlerAdvice.class);

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(value = { IllegalArgumentException.class })
    public Result<?> illegalArgumentExceptionHandler(IllegalArgumentException ex) {
        logger.error("Bad Request: {}", ex.getMessage());
        return Result.fail(ResultType.BAD_REQUEST, "Invalid request parameters.");
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = { jakarta.persistence.EntityNotFoundException.class })
    public Result<?> entityNotFoundExceptionHandler(jakarta.persistence.EntityNotFoundException ex) {
        logger.error("Entity Not Found: {}", ex.getMessage());
        return Result.fail(ResultType.NOT_FOUND, "The requested resource was not found.");
    }

    /**
     * Override parent to return a clean human-readable English message
     * instead of raw JDBC/SQL exception details.
     */
    @Override
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<?> dataIntegrityExceptionHandler(Exception ex) {
        logger.error("Data Integrity Violation: {}", ex.getMessage(), ex);
        return Result.fail(ResultType.SYSTEM_ERROR, "Internal server error. Please try again later.");
    }

    /**
     * Catch-all for any unhandled exception — never leak stack traces to the client.
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(value = { Exception.class })
    public Result<?> fallbackExceptionHandler(Exception ex) {
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);
        return Result.fail(ResultType.SYSTEM_ERROR, "An unexpected error occurred. Please try again later.");
    }
}

