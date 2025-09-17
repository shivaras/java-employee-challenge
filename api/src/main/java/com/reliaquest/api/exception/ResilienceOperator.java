package com.reliaquest.api.exception;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import java.util.function.Function;
import reactor.core.publisher.Mono;

public class ResilienceOperator {
    public static <T> Function<Mono<T>, Mono<T>> withResilience(RateLimiter rateLimiter, Retry retry, T fallbackValue) {

        return mono -> mono
                // Apply rate limiting
                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                // Apply retry on transient errors
                .transformDeferred(RetryOperator.of(retry))
                // Fallback on empty
                .switchIfEmpty(Mono.just(fallbackValue))
                // Fallback on error
                .onErrorResume(ex -> {
                    return Mono.just(fallbackValue);
                });
    }
}
