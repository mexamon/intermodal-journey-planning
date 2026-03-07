package com.thy.cloud.base.core.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.thy.cloud.base.core.exception.AirwayException;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * @author Engin Mahmut
 *         Wrapper for Controller layer for all return objects
 * @version 1.2
 * @apiNote Revised for Validation Infrastructure for I18n
 */
@Tag(name = "book service", description = "Return model of rest request，All rests return objects of this class normally")
@Setter
@Getter
// @SuppressWarnings("all")
@Accessors(chain = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ResultI18n<T> implements Serializable {

    public static final String DEFAULT_ERROR_MESSAGE = "";

    public static final String SUCCESSFUL_CODE = "000000";
    public static final String SUCCESSFUL_MESSAGE = "Successful";
    public static final String FAIL_CODE = "-1";

    @Schema(description = "Result Code")
    @JsonProperty(value = "code")
    private String code;

    @Schema(description = "Result Description Information")
    @JsonProperty(value = "message")
    private String message;

    @Schema(description = "Result Timestamp")
    @JsonProperty(value = "timestamp")
    private Instant timestamp;

    @Schema(description = "Result Data")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(value = "data")
    private transient T data;

    /**
     * Constructor for Instant Timestamp
     */
    public ResultI18n() {
        super();
        this.timestamp = ZonedDateTime.now().toInstant();
    }

    /**
     * @param iResultType Error Types Interface
     */
    public ResultI18n(IResultType iResultType) {
        this.timestamp = ZonedDateTime.now().toInstant();
        this.code = iResultType.getCode();
        this.message = iResultType.getMessage();
    }

    /**
     * @param iResultType Error Types Interface
     * @param data        Data for Errors eg. Validation Errors
     */
    public ResultI18n(IResultType iResultType, T data) {
        this(iResultType);
        this.data = data;
    }

    /**
     * Internal Use for Constructing Successful Results
     *
     * @param code    Error Code
     * @param message Error Message
     * @param data    Error Details
     */
    public ResultI18n(String code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = ZonedDateTime.now().toInstant();
    }

    public static <T> ResultI18n<T> result(String code, String message, T data) {
        return new ResultI18n<>(code, message, data);
    }

    /**
     * Quickly Create and Return Successful Result Data
     *
     * @param data Result Data
     * @return Result
     */
    public static <T> ResultI18n<T> success(T data) {
        return new ResultI18n<>(ResultTypeI18n.SUCCESS, data);
    }

    /**
     * Quickly Create and Return Successful Result Data Without detail
     *
     * @return Result
     */
    public static <T> ResultI18n<T> success() {
        return success(null);
    }

    /**
     * System Exception Class Does Not Return Data
     *
     * @return Result
     */
    public static <T> ResultI18n<T> fail() {
        return new ResultI18n<>(ResultTypeI18n.SYSTEM_ERROR);
    }

    /**
     * Request failure message, according to the type of exception, get different
     * offer messages
     *
     * @param throwable Exception
     * @return RPC call result
     */

    public static ResultI18n<String> fail(IResultType iResultType, Throwable throwable) {
        return fail(iResultType, throwable != null ? throwable.getMessage() : DEFAULT_ERROR_MESSAGE);
    }

    /**
     * System Exception Class and Return Result Data
     *
     * @param data Result Data
     * @return Result
     */
    public static <T> ResultI18n<T> fail(AirwayException baseException, T data) {
        return new ResultI18n<>(baseException.getResultType(), data);
    }

    /**
     * System Exception Class and Without Return Result Data
     *
     * @param baseException Exception Detail
     * @return Result
     */
    public static <T> ResultI18n<T> fail(AirwayException baseException) {
        return fail(baseException, null);
    }

    /**
     * System Exception Class and Return Result Data
     *
     * @param iResultType Error Type
     * @param data        Result Data
     * @return Result
     */
    public static <T> ResultI18n<T> fail(IResultType iResultType, T data) {
        return new ResultI18n<>(iResultType, data);
    }

    /**
     * System Exception Class and Return Result Data
     *
     * @param iResultType Error Type
     * @return Result
     */
    public static ResultI18n<String> fail(IResultType iResultType) {
        // return new ResponseResult<>(iResultType.getCode(), iResultType.getMessage(),
        // null)
        return ResultI18n.fail(iResultType, null);
    }

    /**
     * System Exception Class and Return Result Data
     *
     * @param data Result Data
     * @return Result
     */
    public static <T> ResultI18n<T> fail(T data) {
        return new ResultI18n<>(ResultTypeI18n.SYSTEM_ERROR, data);
    }

    /**
     * Validation Errors will be handled and Without Return Error Messages
     * 
     * @return Result
     */
    public static <T> ResultI18n<T> validateFailed() {
        return new ResultI18n<>(ResultTypeI18n.VALIDATE_FAILED);
    }

    /**
     * Validation Errors will be handled and Returns
     * 
     * @param data Result Data for Validation Errors
     * @return Result
     */
    public static <T> ResultI18n<T> validateFailed(T data) {
        return new ResultI18n<>(ResultTypeI18n.VALIDATE_FAILED, data);
    }

    /**
     * Success code=000000
     *
     * @return true/false
     */
    @JsonIgnore
    public boolean isSuccess() {
        return SUCCESSFUL_CODE.equals(this.code);
    }

    /**
     * Fail
     *
     * @return true/false
     */
    @JsonIgnore
    public boolean isFail() {
        return !isSuccess();
    }

    /**
     * Override ToString
     * 
     * @return Json String
     */
    @Override
    public String toString() {
        return "ResultI18n{code='" + code + "', message='" + message + "', timestamp=" + timestamp + ", data=" + data
                + "}";
    }

}