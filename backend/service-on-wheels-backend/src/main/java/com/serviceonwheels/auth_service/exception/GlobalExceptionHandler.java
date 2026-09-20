package com.serviceonwheels.auth_service.exception;

import com.serviceonwheels.auth_service.dto.ApiResponse;
import com.serviceonwheels.auth_service.security.CorrelationIdContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import com.serviceonwheels.auth_service.exception.PaymentNotFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for centralized, secure error handling across the application.
 * <p>
 * Security invariants:
 * <ul>
 *   <li>NO Java stack traces, class names, file paths, or internal implementation details are returned to API clients.</li>
 *   <li>All responses adhere to the standard {@link ApiResponse} format with a correlation ID.</li>
 *   <li>Full stack traces are preserved exclusively in server-side logs, tied to the request correlation ID.</li>
 *   <li>Authentication errors avoid leaking account existence (preventing user enumeration).</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ApiResponse<Object>> buildResponse(
            HttpStatus status,
            String message,
            Object data,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        if (response != null) {
            response.setHeader(CorrelationIdContext.CORRELATION_ID_HEADER, correlationId);
        }
        return ResponseEntity.status(status)
                .body(ApiResponse.error(message, data, correlationId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationErrors(
            MethodArgumentNotValidException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        Map<String, String> validationErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("[correlationId={}] Validation failed for request [{}]: {}",
                correlationId, request.getRequestURI(), validationErrors);

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", validationErrors, response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] Malformed JSON request body at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.BAD_REQUEST,
                "Malformed JSON request or invalid request body format.", null, response);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(
            BadRequestException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] Bad request at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null, response);
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserAlreadyExists(
            UserAlreadyExistsException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] Registration conflict at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), null, response);
    }

    @ExceptionHandler({InvalidCredentialsException.class, BadCredentialsException.class})
    public ResponseEntity<ApiResponse<Object>> handleInvalidCredentials(
            Exception ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] Invalid credentials at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.UNAUTHORIZED,
                "Invalid email or password. Please try again.", null, response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] Authentication required at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.UNAUTHORIZED,
                "Authentication required. Please provide a valid authentication token.", null, response);
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(
            Exception ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] Access denied at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.FORBIDDEN,
                "Access denied. You do not have permission to access this resource.", null, response);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserNotFound(
            UserNotFoundException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] User not found at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null, response);
    }

    @ExceptionHandler(ServiceRequestNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleServiceRequestNotFound(
            ServiceRequestNotFoundException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] Service request not found at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null, response);
    }

    @ExceptionHandler(MechanicNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleMechanicNotFound(
            MechanicNotFoundException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] Mechanic not found at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null, response);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handlePaymentNotFound(
            PaymentNotFoundException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] Payment not found at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null, response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        String message = "Invalid value for parameter '" + ex.getName() + "'";
        log.warn("[correlationId={}] Type mismatch at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.BAD_REQUEST, message, null, response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingParameter(
            MissingServletRequestParameterException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        String message = "Required query parameter '" + ex.getParameterName() + "' is missing";
        log.warn("[correlationId={}] Missing parameter at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.BAD_REQUEST, message, null, response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        String message = "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.";
        log.warn("[correlationId={}] Method not supported at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, message, null, response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] Resource not found at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.NOT_FOUND, "The requested resource was not found.", null, response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] Illegal argument at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        String message = ex.getMessage() != null ? ex.getMessage() : "Invalid request argument.";
        return buildResponse(HttpStatus.BAD_REQUEST, message, null, response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] Illegal state transition at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        String message = ex.getMessage() != null ? ex.getMessage() : "The requested action cannot be performed in the current state.";
        return buildResponse(HttpStatus.CONFLICT, message, null, response);
    }

    @ExceptionHandler({EmailDeliveryException.class, MailException.class})
    public ResponseEntity<ApiResponse<Object>> handleEmailDelivery(
            Exception ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.error("[correlationId={}] Email delivery failure at [{}]: ",
                correlationId, request.getRequestURI(), ex);

        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to send email at this time. Please try again later.", null, response);
    }

    @ExceptionHandler(InvalidResetTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidResetToken(
            InvalidResetTokenException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.warn("[correlationId={}] Invalid reset token at [{}]: {}",
                correlationId, request.getRequestURI(), ex.getMessage());

        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null, response);
    }

    /**
     * Top-level catch-all handler for unexpected errors.
     * Logs the FULL stack trace with correlationId server-side,
     * but returns ONLY a safe generic error message to API clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        String correlationId = CorrelationIdContext.get();
        log.error("[correlationId={}] Unexpected error while processing request [{}]: ",
                correlationId, request.getRequestURI(), ex);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.", null, response);
    }
}
