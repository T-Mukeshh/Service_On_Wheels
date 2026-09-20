package com.serviceonwheels.auth_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceonwheels.auth_service.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Applies a rate limit to /api/auth/** endpoints based on the client's IP address.
 * Uses Redis for distributed limiting, falls back to in-memory if Redis is unavailable.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, FallbackBucket> fallbackCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    
    private final StringRedisTemplate redisTemplate;

    public RateLimitFilter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/api/auth/")) {
            String ip = getClientIP(request);
            if (!isAllowed(ip)) {
                sendRateLimitError(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String ip) {
        String key = "rate_limit:auth:" + ip;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }
            return count != null && count <= 10;
        } catch (Exception e) {
            // Fallback to in-memory if Redis is down
            return isAllowedInMemory(ip);
        }
    }

    private boolean isAllowedInMemory(String ip) {
        FallbackBucket bucket = fallbackCache.computeIfAbsent(ip, k -> new FallbackBucket());
        long now = System.currentTimeMillis();
        if (now - bucket.windowStart > 60000) {
            bucket.windowStart = now;
            bucket.count.set(0);
        }
        return bucket.count.incrementAndGet() <= 10;
    }

    private static class FallbackBucket {
        long windowStart = System.currentTimeMillis();
        AtomicInteger count = new AtomicInteger(0);
    }

    private void sendRateLimitError(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = "UNKNOWN"; // Fallback if filter order is wrong
        }

        ApiResponse<Void> apiResponse = ApiResponse.error("Too many requests. Please try again later.", null, correlationId);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
