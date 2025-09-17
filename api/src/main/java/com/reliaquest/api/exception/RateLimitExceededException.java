package com.reliaquest.api.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends RuntimeException {
    private final HttpStatus status;

    public HttpStatus getStatus() {
        return status;
    }

    public RateLimitExceededException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
