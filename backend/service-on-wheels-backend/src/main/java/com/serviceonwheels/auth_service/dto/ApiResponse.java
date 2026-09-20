package com.serviceonwheels.auth_service.dto;

import com.serviceonwheels.auth_service.security.CorrelationIdContext;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    @Schema(description = "Indicates if the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response message", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Timestamp of the response", example = "2026-06-12T23:34:16")
    private LocalDateTime timestamp;

    @Schema(description = "Unique correlation ID for tracking request across logs and client", example = "550e8400-e29b-41d4-a716-446655440000")
    private String correlationId;

    @Schema(description = "Response data payload")
    private T data;

    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .correlationId(CorrelationIdContext.get())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(String message, T data, String correlationId) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId != null ? correlationId : CorrelationIdContext.get())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .correlationId(CorrelationIdContext.get())
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, T data, String correlationId) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(LocalDateTime.now())
                .correlationId(correlationId != null ? correlationId : CorrelationIdContext.get())
                .data(data)
                .build();
    }
}
