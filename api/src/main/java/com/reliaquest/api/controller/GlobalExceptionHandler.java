package com.reliaquest.api.controller;

import com.reliaquest.api.exception.EmployeeCreationException;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.RateLimitExceededException;
import com.reliaquest.api.model.EmployeeWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<EmployeeWrapper<String>> employeeNotFound(EmployeeNotFoundException ex) {
        log.info("Error handling employeeNotFound", ex.getMessage());
        EmployeeWrapper<String> wrapper = new EmployeeWrapper<>();
        wrapper.setErrorMessage(ex.getMessage());
        wrapper.setStatus("Failed: Employees not found");
        return ResponseEntity.status(ex.getStatus()).body(wrapper);
    }

    @ExceptionHandler(EmployeeCreationException.class)
    public ResponseEntity<EmployeeWrapper<String>> createEmployeeException(EmployeeCreationException ex) {
        log.info("Error handling createEmployeeException", ex.getMessage());
        EmployeeWrapper<String> wrapper = new EmployeeWrapper<>();
        wrapper.setErrorMessage(ex.getMessage());
        wrapper.setStatus("Failed: Exception Occured in Employee Creation.");
        return ResponseEntity.status(ex.getStatus()).body(wrapper);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<EmployeeWrapper<String>> handleRateLimit(RateLimitExceededException ex) {
        log.info("Error handling handleRateLimit", ex.getMessage());
        EmployeeWrapper<String> wrapper = new EmployeeWrapper<>();
        wrapper.setErrorMessage(ex.getMessage());
        wrapper.setStatus("Failed: Too many requests.");
        return ResponseEntity.status(ex.getStatus()).body(wrapper);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<EmployeeWrapper<String>> handleAllExceptions(Exception ex) {
        log.info("Error handling handleAllExceptions", ex.getMessage());
        EmployeeWrapper<String> wrapper = new EmployeeWrapper<>();
        wrapper.setErrorMessage(ex.getMessage());
        wrapper.setStatus("Failed: Some Internal Exception Occured");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(wrapper);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<EmployeeWrapper<String>> handleEmployeeCreationValidation(
            MethodArgumentNotValidException ex) {
        log.info("Error handling handleEmployeeCreationValidation", ex.getMessage());
        EmployeeWrapper<String> wrapper = new EmployeeWrapper<>();
        wrapper.setStatus("Failed: Employee Validation failed");
        wrapper.setErrorMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(wrapper);
    }
}
