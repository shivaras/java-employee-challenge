package com.reliaquest.api.client;

import com.reliaquest.api.exception.EmployeeCreationException;
import com.reliaquest.api.exception.EmployeeNotFoundException;
import com.reliaquest.api.exception.RateLimitExceededException;
import com.reliaquest.api.exception.ResilienceOperator;
import com.reliaquest.api.model.EmployeeDTO;
import com.reliaquest.api.model.EmployeeInput;
import com.reliaquest.api.model.EmployeeWrapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeWebClient {

    @Autowired
    private final WebClient webClient;

    @Autowired
    private final Retry employeeRetry;

    @Autowired
    private final RateLimiter employeeApiLimiter;

    private final ParameterizedTypeReference<EmployeeWrapper<List<EmployeeDTO>>> LIST_WRAPPER =
            new ParameterizedTypeReference<>() {};
    private final ParameterizedTypeReference<EmployeeWrapper<EmployeeDTO>> SINGLE_WRAPPER =
            new ParameterizedTypeReference<>() {};
    private final ParameterizedTypeReference<EmployeeWrapper<Boolean>> BOOL_WRAPPER =
            new ParameterizedTypeReference<>() {};

    public Mono<List<EmployeeDTO>> getAllEmployees() {
        log.info("inside getAllEmployees of EmployeeWebClient");
        return webClient
                .get()
                .uri("/employee")
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.TOO_MANY_REQUESTS.value(), resp -> resp.bodyToMono(
                                String.class)
                        .flatMap(msg -> Mono.error(new RateLimitExceededException(HttpStatus.TOO_MANY_REQUESTS, msg))))
                .onStatus(
                        status -> status.value() == HttpStatus.NOT_FOUND.value(), resp -> resp.bodyToMono(String.class)
                                .flatMap(msg -> Mono.error(new EmployeeNotFoundException(HttpStatus.NOT_FOUND, msg))))
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .flatMap(msg -> Mono.error(new RuntimeException(msg))))
                .bodyToMono(LIST_WRAPPER)
                .map(EmployeeWrapper::getData)
                .doOnNext(data -> log.info("✅ Deserialized Employees: {}", data))
                .transformDeferred(ResilienceOperator.withResilience(
                        employeeApiLimiter,
                        employeeRetry,
                        List.of(new EmployeeDTO("0", "Fallback Employee", 0, 0, "N/A", "fallback@example.com"))));
    }

    public Mono<EmployeeDTO> getEmployeeById(String id) {
        return webClient
                .get()
                .uri("/employee/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.TOO_MANY_REQUESTS.value(), resp -> resp.bodyToMono(
                                String.class)
                        .flatMap(msg -> Mono.error(new RateLimitExceededException(HttpStatus.TOO_MANY_REQUESTS, msg))))
                .onStatus(
                        status -> status.value() == HttpStatus.NOT_FOUND.value(), resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("Employees not found.")
                                .flatMap(msg -> Mono.error(new EmployeeNotFoundException(HttpStatus.NOT_FOUND, msg))))
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .flatMap(msg -> Mono.error(new RuntimeException(msg))))
                .bodyToMono(SINGLE_WRAPPER)
                .map(EmployeeWrapper::getData)
                .doOnNext(data -> log.info("✅ Deserialized Employee: {}", data))
                .transformDeferred(ResilienceOperator.withResilience(
                        employeeApiLimiter,
                        employeeRetry,
                        new EmployeeDTO("0", "Fallback Employee", 0, 0, "N/A", "fallback@example.com")));
    }

    public Mono<EmployeeDTO> createEmployee(EmployeeInput req, String idempotencyKey) {
        log.info("inside createEmployee method");
        WebClient.RequestBodySpec spec = webClient.post().uri("/employee");
        if (idempotencyKey != null) spec.header("Idempotency-Key", idempotencyKey);
        log.info(idempotencyKey);
        return spec.bodyValue(req)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.TOO_MANY_REQUESTS.value(), resp -> resp.bodyToMono(
                                String.class)
                        .flatMap(msg -> Mono.error(new RateLimitExceededException(HttpStatus.TOO_MANY_REQUESTS, msg))))
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .defaultIfEmpty("Employee not created, Some exception occurred")
                        .flatMap(msg ->
                                Mono.error(new EmployeeCreationException(HttpStatus.INTERNAL_SERVER_ERROR, msg))))
                .bodyToMono(SINGLE_WRAPPER)
                .map(EmployeeWrapper::getData)
                .doOnNext(data -> log.info("✅ Deserialized Employee: {}", data))
                .transformDeferred(RetryOperator.of(employeeRetry));
    }

    public Mono<Boolean> deleteEmployeeByName(EmployeeDTO req) {
        Map<String, String> reqBody = Map.of("name", req.getName());
        WebClient.RequestBodySpec spec = (RequestBodySpec) webClient.delete().uri("/employee");
        return spec.bodyValue(reqBody)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.TOO_MANY_REQUESTS.value(), resp -> resp.bodyToMono(
                                String.class)
                        .flatMap(msg -> Mono.error(new RateLimitExceededException(HttpStatus.TOO_MANY_REQUESTS, msg))))
                .onStatus(
                        status -> status.value() == HttpStatus.NOT_FOUND.value(), resp -> resp.bodyToMono(String.class)
                                .flatMap(msg -> Mono.error(new EmployeeNotFoundException(HttpStatus.NOT_FOUND, msg))))
                .onStatus(HttpStatusCode::isError, resp -> resp.bodyToMono(String.class)
                        .flatMap(msg -> Mono.error(new RuntimeException(msg))))
                .bodyToMono(BOOL_WRAPPER)
                .map(EmployeeWrapper::getData)
                .doOnNext(data -> log.info("✅ Deserialized Employee: {}", data))
                .transformDeferred(RetryOperator.of(employeeRetry));
    }
}
