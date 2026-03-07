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
 * System Error Types Enums I18n
 * @author Engin Mahmut
 * @version 1.1
 */
@ToString
@Getter
@AllArgsConstructor
public enum ResultTypeI18n implements IResultType {

    SUCCESS("000000", "api.system.success"),

    SYSTEM_ERROR("-1", "api.system.error"),
    SYSTEM_BUSY("000001", "api.system.busy"),

    VALIDATE_FAILED("000002", "api.validate.failed"),
    ARGUMENT_NOT_VALID("000003", "api.argument.not.valid"),
    INTERNAL_RESOURCE_ERROR("000004", "api.internal.resource.error"),
    NO_HANDLER_FOUND("000005", "api.no.handler.found"),
    JSON_PARSE_ERROR("000006", "api.json.parse.error"),

    SERVICE_DOWNGRADE_ERROR("000007", "api.service.downgrade.error"),

    METHOD_NOT_ALLOWED("405000", "api.method.not.allowed"),
    NOT_FOUND("404000", "api.not.found"),
    BAD_REQUEST("400000", "api.bad.request"),

    UNSUPPORTED_MEDIA_TYPE("050006","api.unsupported.media.type"),

    GATEWAY_NOT_FOUND_SERVICE("010404", "api.gateway.not.found.service"),
    GATEWAY_ERROR("010500", "api.getaway.error"),
    GATEWAY_CONNECT_TIME_OUT("010002", "api.gateway.connect.time.out"),
    GATEWAY_IP_BLOCKED("010002", "api.gateway.ip.blocked"),

    UPLOAD_FILE_SIZE_LIMIT("020001", "api.upload.file.size.limit"),
    UPLOAD_NO_AVAILABLE_FILE_HANDLER("020002", "api.upload.no.available.file.handler"),
    UPLOAD_REPEAT_TYPE("020003", "api.upload.repeat.type"),
    UPLOAD_ATTACHMENT_NOT_NULL("020004", "api.upload.attachment.not.null"),
    UPLOAD_FAILED("020005", "api.upload.failed"),

    UNIQUE_KEY_CONFLICT("030000","api.unique.key.conflict"),

    USER_NOT_FOUND("040000","api.user.not.found"),
    USER_PASSWORD_ERROR("040001","api.user.password.error"),
    TOKEN_FAIL("040002","api.token.fail"),
    LOGIN_VERIFY_FALL("040003", "api.login.verify.fall"),
    AUTH_FAILED("040004", "api.auth.failed"),
    ACCESS_DENIED("040005", "api.access.denied"),
    OAUTH_CLIENT_DETAILS_FAILED("040006", "api.oauth.client.details.failed"),
    OAUTH_CLIENT_DETAILS_EXPIRED("040007", "api.oauth.client.details.expired"),


    PARAM_VERIFY_FALL("050000", "api.param.verify.fall"),
    NO_DATA("050001", "api.no.data"),
    DATA_CHANGE("050002", "api.data.change"),
    DATA_EXIST("050003", "api.data.exist"),
    DUPLICATE_PRIMARY_KEY("050004","api.duplicate.primary.key"),
    DATA_INTEGRITY_VIOLATION("050005","api.data.integrity.violation"),
    CONSTRAINT_VIOLATION("050006","api.constraint.violation"),

    ELASTICSEARCH_NOT_EMPTY("060001","api.elasticsearch.not.empty"),

    //Distributed Lock
    DIST_LOCK_FAILURE("070001", "api.dist.lock.failure"),
    DIST_LOCK_SUCCESS("070002", "api.dist.lock.success"),
    DIST_LOCK_MANY_REQ("070003", "api.dist.lock.many.req"),
    DIST_LOCK_NOT_ENOUGH_STOCK("070004", "api.dist.lock.not.enough.stock");

    /**
     * Error Type Code
     */
    private final String code;

    /**
     * Error Type Description
     */
    private final String message;


    public static ResultTypeI18n codeOf(String code) {
        for (ResultTypeI18n state : values()) {
            if (state.getCode().equals(code)) {
                return state;
            }
        }
        return null;
    }

}
