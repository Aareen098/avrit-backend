package com.hireflow.common;

import org.springframework.http.HttpStatus;

// Throw this anywhere to send { "message": "..." } with the given status.
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}