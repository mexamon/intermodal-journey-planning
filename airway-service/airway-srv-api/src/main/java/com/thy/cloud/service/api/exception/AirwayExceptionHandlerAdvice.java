package com.thy.cloud.service.api.exception;

import com.thy.cloud.base.core.api.Result;
import com.thy.cloud.base.core.api.ResultType;
import com.thy.cloud.base.core.exception.handler.GlobalExceptionHandlerDefaultAdvice;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        return Result.fail(ResultType.BAD_REQUEST, getMessage(ex));
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(value = { jakarta.persistence.EntityNotFoundException.class })
    public Result<?> entityNotFoundExceptionHandler(jakarta.persistence.EntityNotFoundException ex) {
        logger.error("Entity Not Found: {}", ex.getMessage());
        return Result.fail(ResultType.NOT_FOUND, getMessage(ex));
    }
}
