package com.reliaquest.api.exception;

import org.springframework.http.HttpStatus;

public class EmployeeNotFoundException extends RuntimeException {

    private final HttpStatus status;

    public HttpStatus getStatus() {
        return status;
    }

    public EmployeeNotFoundException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
