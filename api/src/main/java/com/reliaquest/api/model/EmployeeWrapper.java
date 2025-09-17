package com.reliaquest.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class EmployeeWrapper<T> {

    private T data;
    private String errorMessage;
    private String status;
}
