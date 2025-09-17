package com.reliaquest.api.service;

import com.reliaquest.api.client.EmployeeWebClient;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.model.EmployeeDTO;
import com.reliaquest.api.model.EmployeeInput;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    @Autowired
    private final EmployeeWebClient client;

    private final ConcurrentHashMap<String, EmployeeDTO> idempotencyMap = new ConcurrentHashMap<>();

    public Mono<List<EmployeeDTO>> getAllEmployees() {
        log.info("List getAllEmployees: ");
        return client.getAllEmployees();
    }

    public Mono<List<EmployeeDTO>> getEmployeesByNameSearch(String fragment) {
        return client.getAllEmployees()
                .map(list -> list.stream()
                        .filter(e ->
                                e.getName() != null && e.getName().toLowerCase().contains(fragment.toLowerCase()))
                        .collect(Collectors.toList()))
                .flatMap(filteredList -> {
                    if (filteredList.isEmpty()) {
                        return Mono.error(new EmployeeNotFoundException(
                                HttpStatus.NOT_FOUND, "No employee found with name containing: " + fragment));
                    }
                    return Mono.just(filteredList);
                });
    }

    public Mono<EmployeeDTO> getEmployeeById(String id) {
        log.info("List getEmployeeById: ");
        return client.getEmployeeById(id);
    }

    public Integer getHighestSalaryOfEmployees() {
        log.info("List getHighestSalaryOfEmployees: ");
        List<EmployeeDTO> employees = client.getAllEmployees().block(); // unwrap List<EmployeeDTO>
        log.info("List size: ", employees.size());
        Integer sal = employees.stream()
                .map(EmployeeDTO::getSalary)
                .max(Integer::compareTo)
                .orElseThrow(() -> new EmployeeNotFoundException(HttpStatus.NOT_FOUND, "No salaries found"));
        log.info("Highest Salary: ", sal);
        return sal;
    }

    public List<String> getTop10HighestEarningEmployeeNames() {
        List<EmployeeDTO> employees = client.getAllEmployees().block();
        return employees.stream()
                .sorted(Comparator.comparing(EmployeeDTO::getSalary, Comparator.reverseOrder()))
                .limit(10)
                .map(EmployeeDTO::getName)
                .toList();
    }

    public Mono<EmployeeDTO> createEmployee(String idempotencyKey, EmployeeInput req) {
        // idempotency check
        if (idempotencyKey != null) {
            EmployeeDTO cached = idempotencyMap.get(idempotencyKey);
            if (cached != null) {
                log.info("Idempotency hit for key={}", idempotencyKey);
                return Mono.just(cached);
            }
        }

        return client.createEmployee(req, idempotencyKey)
                .doOnSubscribe(s -> log.debug("Calling mock server to create employee: {}", req))
                .onErrorResume(ex -> {
                    log.error("Create employee failed after retries: {}", ex.toString());
                    EmployeeDTO fallback = new EmployeeDTO();
                    fallback.setId("fallback-" + UUID.randomUUID());
                    fallback.setName(req.getName() + " (pending)");
                    fallback.setSalary(req.getSalary());
                    fallback.setAge(req.getAge());
                    fallback.setTitle(req.getTitle());
                    return Mono.just(fallback);
                })
                .doOnNext(created -> {
                    log.info(
                            "Employee created (or fallback returned) id={}, name={}",
                            created.getId(),
                            created.getName());
                    if (idempotencyKey != null) idempotencyMap.put(idempotencyKey, created);
                });
    }

    public Mono<String> deleteEmployeeById(String id) {
        log.info("Inside deleteEmployeeById: ");
        EmployeeDTO empDTO = client.getEmployeeById(id).block();
        return client.deleteEmployeeByName(empDTO)
                .doOnError(err -> log.error("❌ Error in deleteEmployeeById pipeline", err))
                .doOnSuccess(name -> log.info("Pipeline completed, returning {}", name))
                .flatMap(success -> {
                    if (!success) return Mono.error(new RuntimeException("Delete returned false"));
                    return Mono.just(empDTO.getName());
                });
    }
}
