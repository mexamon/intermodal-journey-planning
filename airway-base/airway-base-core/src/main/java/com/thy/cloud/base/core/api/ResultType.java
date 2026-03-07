/*
 * Copyright (c) 2017-2020 DENK-IT All rights reserved.
 *
 * http://www.denk-it.com
 *
 */
package com.thy.cloud.base.core.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * System Error Types Enums
 * @author Engin Mahmut
 * @version 1.1
 */

@ToString
@Getter
@AllArgsConstructor
public enum ResultType implements IResultType {


    SYSTEM_ERROR("-1", "System Exception"),
    SYSTEM_BUSY("000001", "The System is Busy, Please Try Again Later"),

    VALIDATE_FAILED("000002", "Validation Failed"),
    ARGUMENT_NOT_VALID("000003", "Request Parameter Verification Failed"),
    INTERNAL_RESOURCE_ERROR("000004", "Internal Resource Error"),
    NO_HANDLER_FOUND("000005", "No Handler Found"),
    JSON_PARSE_ERROR("000006", "Json Parse Error"),

    SERVICE_DOWNGRADE_ERROR("000007", "Service Downgrade Error"),

    METHOD_NOT_ALLOWED("405000", "Method Not Allowed"),
    NOT_FOUND("404000", "Not Found"),
    BAD_REQUEST("400000", "Bad Request"),

    UNSUPPORTED_MEDIA_TYPE("050006","Content Type Not Supported"),

    GATEWAY_NOT_FOUND_SERVICE("010404", "Service Not Found"),
    GATEWAY_ERROR("010500", "Gateway Exception"),
    GATEWAY_CONNECT_TIME_OUT("010002", "Gateway Timeout"),
    GATEWAY_IP_BLOCKED("010002", "Gateway IP Blocked"),

    UPLOAD_FILE_SIZE_LIMIT("020001", "Upload File Size Exceeds Limit"),
    UPLOAD_NO_AVAILABLE_FILE_HANDLER("020002", "No available file handlers to operate the file"),
    UPLOAD_REPEAT_TYPE("020003", "Same attachment type implements must be unique"),
    UPLOAD_ATTACHMENT_NOT_NULL("020004", "Attachment must not be null"),
    UPLOAD_FAILED("020005", "Failed to upload file"),

    UNIQUE_KEY_CONFLICT("030000","Unique Key Conflict"),

    USER_NOT_FOUND("040000","User Not Found"),
    USER_PASSWORD_ERROR("040001","User Password Error"),
    TOKEN_FAIL("040002","Token Failed"),
    LOGIN_VERIFY_FALL("040003", "Login invalid"),
    AUTH_FAILED("040004", "Authentication Verification Failed"),
    ACCESS_DENIED("040005", "Access Denied"),
    OAUTH_CLIENT_DETAILS_FAILED("040006", "Oauth Client Details Verification Failed"),
    OAUTH_CLIENT_DETAILS_EXPIRED("040007", "Oauth Client Details Expired"),


    PARAM_VERIFY_FALL("050000", "Parameter Verification Error"),
    NO_DATA("050001", "No related data"),
    DATA_CHANGE("050002", "There is no change in the data"),
    DATA_EXIST("050003", "Data already exists"),
    DUPLICATE_PRIMARY_KEY("050004","Duplicate Primary Key"),
    DATA_INTEGRITY_VIOLATION("050005","Data Integrity Violation"),
    CONSTRAINT_VIOLATION("050006","Constraint Violation"),
    DOMAIN_NULL("050007","Domain Null"),

    ELASTICSEARCH_NOT_EMPTY("060001","Elasticsearch Model Can Not Empty"),

    //Distributed Lock
    DIST_LOCK_FAILURE("070001", "Failed to acquire lock"),
    DIST_LOCK_SUCCESS("070002", "Acquire the lock successfully"),
    DIST_LOCK_MANY_REQ("070003", "Too many requests"),
    DIST_LOCK_NOT_ENOUGH_STOCK("070004", "Not enough stock"),

    INFORMER_MESSAGING_FAIL("080004","Informer Messaging Fail");

    /*OK("200", "Success."),
    INTERNAL_SERVER_ERROR("500", "System exception."),
    BAD_REQUEST("400","The parameter is invalid."),
    UNAUTHORIZED("401","No relevant permissions."),
    TOKEN_CHECK_FAIL("403","Token verification failed (Token expired or incorrect.)"),
    FORBIDDEN("403","Access denied."),
    NOT_FOUND("404", "Not found."),
    METHOD_NOT_ALLOWED("405", "Method not supported."),
    ILLEGAL_ACCESS("406", "Illegal acquisition."),
    REPEAT_COMMIT("111", "Repeated submission."),*/

    /**
     * Error Type Code
     */
    private final String code;

    /**
     * Error Type Description
     */
    private final String message;


    public static ResultType codeOf(String code) {
        for (ResultType state : values()) {
            if (state.getCode().equals(code)) {
                return state;
            }
        }
        return null;
    }

}
