package com.reliaquest.api.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ApiConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder, @Value("${webclient.employee.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public Retry employeeRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("employeeRetry");
    }

    @Bean
    public RateLimiter employeeApiLimiter(RateLimiterRegistry rateLimiterRegistry) {
        return rateLimiterRegistry.rateLimiter("employeeApiLimiter");
    }
}
