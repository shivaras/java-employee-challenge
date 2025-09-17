package com.reliaquest.api.controller;

import com.reliaquest.api.model.EmployeeDTO;
import com.reliaquest.api.model.EmployeeInput;
import com.reliaquest.api.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController implements IEmployeeController<EmployeeDTO, EmployeeInput> {

    @Autowired
    private final EmployeeService service;

    @Override
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        List<EmployeeDTO> employees = service.getAllEmployees().block();
        return ResponseEntity.ok(employees);
    }

    @Override
    public ResponseEntity<List<EmployeeDTO>> getEmployeesByNameSearch(String searchString) {
        List<EmployeeDTO> employees =
                service.getEmployeesByNameSearch(searchString).block();
        return ResponseEntity.ok(employees);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ResponseEntity getEmployeeById(String id) {
        log.info("inside getEmployeeById  " + id);
        if ("highestSalary".equalsIgnoreCase(id.trim())) {
            log.info("inside if getHighestSalaryOfEmployees");
            return getHighestSalaryOfEmployees();
        } else if ("topTenHighestEarningEmployeeNames".equalsIgnoreCase(id.trim())) {
            log.info("inside if topTenHighestEarningEmployeeNames");
            return getTopTenHighestEarningEmployeeNames();
        } else {
            EmployeeDTO employee = service.getEmployeeById(id).block();
            return ResponseEntity.ok(employee);
        }
    }

    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        return ResponseEntity.ok(service.getHighestSalaryOfEmployees());
    }

    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        return ResponseEntity.ok(service.getTop10HighestEarningEmployeeNames());
    }

    @Override
    public ResponseEntity<EmployeeDTO> createEmployee(@Valid @RequestBody EmployeeInput request) {
        log.info("Received createEmployee name={}" + request.getName());
        return service.createEmployee(UUID.randomUUID().toString(), request)
                .map(emp -> ResponseEntity.status(HttpStatus.CREATED).body(emp))
                .block();
    }

    @Override
    public ResponseEntity<String> deleteEmployeeById(String id) {
        String name = service.deleteEmployeeById(id).block();
        return ResponseEntity.ok(name);
    }
}
