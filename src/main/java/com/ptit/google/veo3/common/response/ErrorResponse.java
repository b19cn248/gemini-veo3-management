package com.ptit.google.veo3.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse extends ApiResponse<Object> {
    private int status;
    private String error;
    private String path;
    private Map<String, List<String>> validationErrors;

    public ErrorResponse() {
        super(false, null, null);
    }

    public ErrorResponse(HttpStatus httpStatus, String message, String path) {
        super(false, message, null);
        this.status = httpStatus.value();
        this.error = httpStatus.getReasonPhrase();
        this.path = path;
    }

    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(status, message, path);
    }

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status, message, null);
    }

    public ErrorResponse withValidationErrors(Map<String, List<String>> validationErrors) {
        this.validationErrors = validationErrors;
        return this;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, List<String>> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Map<String, List<String>> validationErrors) {
        this.validationErrors = validationErrors;
    }
}