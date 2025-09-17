package com.reliaquest.api.exception;

import org.springframework.http.HttpStatus;

public class EmployeeCreationException extends RuntimeException {

    private final HttpStatus status;

    public HttpStatus getStatus() {
        return status;
    }

    public EmployeeCreationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
