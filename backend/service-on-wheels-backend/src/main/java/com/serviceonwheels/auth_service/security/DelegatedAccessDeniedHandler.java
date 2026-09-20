package com.serviceonwheels.auth_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceonwheels.auth_service.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles unauthorized requests (insufficient privileges) by returning a standardized JSON error response with HTTP 403.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DelegatedAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        String correlationId = CorrelationIdContext.get();
        log.warn("[{}] Access denied for request [{}]: {}",
                correlationId, request.getRequestURI(), accessDeniedException.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(CorrelationIdContext.CORRELATION_ID_HEADER, correlationId);

        ApiResponse<Object> apiResponse = ApiResponse.error(
                "Access denied. You do not have permission to access this resource.",
                null,
                correlationId
        );

        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
}
