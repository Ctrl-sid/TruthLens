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
        // Apply rate limit specifically to verification & NLP endpoints
        if (path.startsWith("/api/verify") || path.startsWith("/api/nlp")) {
            String clientIp = extractClientIp(request);
            long now = System.currentTimeMillis();

            ClientBucket bucket = clientBuckets.compute(clientIp, (key, existing) -> {
                if (existing == null || (now - existing.windowStart) > ONE_MINUTE_MILLIS) {
                    return new ClientBucket(now);
                } else {
                    existing.requestCount.incrementAndGet();
                    return existing;
                }
            });

            if (bucket.requestCount.get() > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for IP: {} on URI: {}", clientIp, path);
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
