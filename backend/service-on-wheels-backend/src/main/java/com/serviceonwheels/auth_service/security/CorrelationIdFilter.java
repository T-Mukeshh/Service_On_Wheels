package com.serviceonwheels.auth_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filter that establishes a Correlation ID for every incoming HTTP request.
 * <p>
 * If an incoming 'X-Correlation-ID' header is provided and valid, it is reused;
 * otherwise a secure UUID is generated.
 * The ID is added to SLF4J MDC, the response headers, and request attributes.
 * MDC is reliably cleaned up in a finally block to prevent thread-pool pollution.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final Pattern SAFE_CORRELATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);

        MDC.put(CorrelationIdContext.MDC_KEY, correlationId);
        request.setAttribute(CorrelationIdContext.CORRELATION_ID_HEADER, correlationId);
        response.setHeader(CorrelationIdContext.CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CorrelationIdContext.MDC_KEY);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String headerValue = request.getHeader(CorrelationIdContext.CORRELATION_ID_HEADER);
        if (headerValue != null) {
            String trimmed = headerValue.trim();
            if (!trimmed.isEmpty() && SAFE_CORRELATION_ID_PATTERN.matcher(trimmed).matches()) {
                return trimmed;
            }
        }
        return UUID.randomUUID().toString();
    }
}
