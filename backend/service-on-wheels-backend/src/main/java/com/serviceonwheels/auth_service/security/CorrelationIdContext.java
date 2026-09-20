package com.serviceonwheels.auth_service.security;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility context to access the current request's Correlation ID.
 * Retrieves from SLF4J MDC first, with a fallback to UUID generation if outside a web request.
 */
public final class CorrelationIdContext {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    private CorrelationIdContext() {
        // Utility class
    }

    /**
     * Gets the active correlation ID for the current thread / request.
     * If not found in MDC, generates a new random UUID.
     *
     * @return non-null, non-blank correlation ID
     */
    public static String get() {
        String correlationId = MDC.get(MDC_KEY);
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        return UUID.randomUUID().toString();
    }
}
