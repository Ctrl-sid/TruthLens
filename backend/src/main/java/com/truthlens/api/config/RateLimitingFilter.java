package com.truthlens.api.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final int MAX_AUTH_REQUESTS_PER_MINUTE = 10;
    private static final long ONE_MINUTE_MILLIS = 60_000L;

    private static class ClientBucket {
        long windowStart;
        AtomicInteger requestCount;

        ClientBucket(long windowStart) {
            this.windowStart = windowStart;
            this.requestCount = new AtomicInteger(1);
        }
    }

    private final Map<String, ClientBucket> clientBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientIp = extractClientIp(request);
        long now = System.currentTimeMillis();

        // 1. Auth Endpoint Protection (Brute Force / Credential Stuffing Mitigation)
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register")) {
            String bucketKey = "auth_" + clientIp;
            ClientBucket bucket = clientBuckets.compute(bucketKey, (key, existing) -> {
                if (existing == null || (now - existing.windowStart) > ONE_MINUTE_MILLIS) {
                    return new ClientBucket(now);
                } else {
                    existing.requestCount.incrementAndGet();
                    return existing;
                }
            });

            if (bucket.requestCount.get() > MAX_AUTH_REQUESTS_PER_MINUTE) {
                log.warn("Auth rate limit exceeded for IP: {} on URI: {}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.setHeader("Retry-After", "60");
                response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded on authentication. Maximum 10 attempts per minute allowed to prevent brute-force attacks.\",\"retryAfterSeconds\":60}");
                return;
            }
        }

        // 2. Verification & NLP Endpoints Protection
        if (path.startsWith("/api/verify") || path.startsWith("/api/nlp")) {
            String bucketKey = "verify_" + clientIp;
            ClientBucket bucket = clientBuckets.compute(bucketKey, (key, existing) -> {
                if (existing == null || (now - existing.windowStart) > ONE_MINUTE_MILLIS) {
                    return new ClientBucket(now);
                } else {
                    existing.requestCount.incrementAndGet();
                    return existing;
                }
            });

            if (bucket.requestCount.get() > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Verification rate limit exceeded for IP: {} on URI: {}", clientIp, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.setHeader("Retry-After", "60");
                response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Maximum 30 requests per minute allowed.\",\"retryAfterSeconds\":60}");
                return;
            }

            // Periodic cleanup of stale IPs
            if (clientBuckets.size() > 5000) {
                clientBuckets.entrySet().removeIf(entry -> (now - entry.getValue().windowStart) > ONE_MINUTE_MILLIS * 2);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "UNKNOWN";
    }
}
