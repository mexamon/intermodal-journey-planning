package com.thy.cloud.base.core.exception;

import com.thy.cloud.base.core.api.IResultType;
import com.thy.cloud.base.core.api.ResultType;
import lombok.Getter;

import java.io.Serializable;

/**
 * @author Engin Mahmut
 * Airway Exception Standardization Model
 * @version 1.2
 */
@Getter
public class AirwayException extends RuntimeException implements Serializable {

    private static final long serialVersionUID = 1905122041950251207L;

    /**
     * The Type of Error Corresponding to The Exception
     */
    private final transient ResultType resultType;

    /**
     * The Default is System Exception
     */
    public AirwayException() {
        this.resultType = ResultType.SYSTEM_ERROR;
    }

    /**
     * The Default is System Exception with Message
     */
    public AirwayException(String message) {
        super(message);
        this.resultType = ResultType.SYSTEM_ERROR;
    }

    /**
     * Known ErrorCode Exception
     */
    public AirwayException(ResultType resultType) {
        super(resultType.getMessage());
        this.resultType = resultType;
    }

    /**
     * Known ErrorCode Exception with Message
     */
    public AirwayException(ResultType resultType, String msg) {
        super(msg);
        this.resultType = resultType;
    }


    /**
     * Known ErrorCode Exception with Throwable Cause
     */
    public AirwayException(ResultType resultType, Throwable cause)
    {
        super(cause);
        this.resultType = resultType;
    }

    /**
     * Known ErrorCode Exception with Message and Throwable Cause
     */
    public AirwayException(ResultType resultType, String message, Throwable cause)
    {
        super(message,cause);
        this.resultType = resultType;
    }


    public String getCode() {
        return resultType.getCode();
    }

    public String getMsg() {
        return resultType.getMessage();
    }

    /**
     * Improve performance
     *
     * @return Throwable
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    public Throwable doFillInStackTrace() {
        return super.fillInStackTrace();
    }

    public IResultType getResultType() {
        return this.resultType;
    }

}