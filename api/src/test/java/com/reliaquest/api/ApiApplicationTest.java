package com.reliaquest.api;

import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.RateLimitExceededException;
import com.reliaquest.api.model.EmployeeDTO;
import com.reliaquest.api.model.EmployeeInput;
import com.reliaquest.api.service.EmployeeService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiApplicationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private EmployeeService service;

    private EmployeeDTO emp1;
    private EmployeeDTO emp2;

    @BeforeEach
    void setup() {
        emp1 = new EmployeeDTO(
                "c290df07-d253-4fec-b3fc-0630ab2e479d", "John Doe", 5000, 30, "Engineer", "john@company.com");
        emp2 = new EmployeeDTO(
                "c290df07-d253-4fec-b3fc-0630ab2e479e", "Jane Doe", 7000, 28, "Manager", "jane@company.com");
    }

    @Test
    void testGetAllEmployees() {
        Mockito.when(service.getAllEmployees()).thenReturn(Mono.just(List.of(emp1, emp2)));

        webTestClient
                .get()
                .uri("/api")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$[0].employee_name")
                .isEqualTo("John Doe")
                .jsonPath("$[1].employee_name")
                .isEqualTo("Jane Doe");
    }

    @Test
    void testCreateEmployee() {
        EmployeeInput input = new EmployeeInput("Mark", 6000, 35, "Analyst");
        Mockito.when(service.createEmployee(Mockito.anyString(), Mockito.any(EmployeeInput.class)))
                .thenReturn(Mono.just(emp1));

        webTestClient
                .post()
                .uri("/api")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(input)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectBody()
                .jsonPath("$.employee_name")
                .isEqualTo("John Doe")
                .jsonPath("$.employee_salary")
                .isEqualTo(5000);
    }

    @Test
    void testGetEmployeeById() {

        Mockito.when(service.getEmployeeById("c290df07-d253-4fec-b3fc-0630ab2e479e"))
                .thenReturn(Mono.just(emp2));

        webTestClient
                .get()
                .uri("/api/{id}", "c290df07-d253-4fec-b3fc-0630ab2e479e")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.employee_name")
                .isEqualTo("Jane Doe")
                .jsonPath("$.employee_age")
                .isEqualTo(28);
    }

    @Test
    void testGetEmployeeByIdNotFound() {

        Mockito.when(service.getEmployeeById("c290df07-d253-4fec-b3fc-0630ab2e4777"))
                .thenReturn(Mono.error(new EmployeeNotFoundException(HttpStatus.NOT_FOUND, "Employee not found")));

        webTestClient
                .get()
                .uri("/api/{id}", "c290df07-d253-4fec-b3fc-0630ab2e4777")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("Failed: Employees not found");
    }

    @Test
    void testCreateEmployeeValidationError() {
        EmployeeInput input = new EmployeeInput();

        webTestClient
                .post()
                .uri("/api")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(input)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("Failed: Employee Validation failed");
    }

    @Test
    void testRateLimitExceeded() {

        Mockito.when(service.getEmployeeById("c290df07-d253-4fec-b3fc-0630ab2e479e"))
                .thenReturn(Mono.error(new RateLimitExceededException(
                        HttpStatus.TOO_MANY_REQUESTS, "Too many requests, please try again later")));

        webTestClient
                .get()
                .uri("/api/{id}", "c290df07-d253-4fec-b3fc-0630ab2e479e")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .is4xxClientError()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo("Failed: Too many requests.");
    }
}
