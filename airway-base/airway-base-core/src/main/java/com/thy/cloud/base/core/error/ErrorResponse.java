package com.thy.cloud.base.core.error;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Engin Mahmut
 *         Error Items Collection
 * @version 1.1
 */

@SuppressWarnings("unused")
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ErrorResponse {
    @JsonProperty(value = "errors")
    private List<ErrorItem> errors = new ArrayList<>();

    public List<ErrorItem> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorItem> errors) {
        this.errors = errors;
    }

    public void addError(ErrorItem error) {
        this.errors.add(error);
    }
}