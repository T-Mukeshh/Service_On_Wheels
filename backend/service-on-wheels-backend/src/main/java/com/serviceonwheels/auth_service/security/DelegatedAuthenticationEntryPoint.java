package com.serviceonwheels.auth_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serviceonwheels.auth_service.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles unauthenticated requests by returning a standardized JSON error response with HTTP 401.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DelegatedAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        String correlationId = CorrelationIdContext.get();
        log.warn("[{}] Authentication failure for request [{}]: {}",
                correlationId, request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(CorrelationIdContext.CORRELATION_ID_HEADER, correlationId);

        ApiResponse<Object> apiResponse = ApiResponse.error(
                "Authentication required. Please provide a valid authentication token.",
                null,
                correlationId
        );

        objectMapper.writeValue(response.getOutputStream(), apiResponse);
    }
}
