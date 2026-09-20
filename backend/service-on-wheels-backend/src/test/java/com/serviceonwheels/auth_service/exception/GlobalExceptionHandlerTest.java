package com.serviceonwheels.auth_service.exception;

import com.serviceonwheels.auth_service.dto.ApiResponse;
import com.serviceonwheels.auth_service.security.CorrelationIdContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        response = new MockHttpServletResponse();
        MDC.put(CorrelationIdContext.MDC_KEY, "test-correlation-id-999");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void handleGenericException_returnsHttp500AndSafeMessageWithoutStackTrace() {
        // Intentionally thrown sensitive exception containing internal details
        RuntimeException secretException = new RuntimeException("DB connection failure to mongodb://admin:secret@10.0.0.1/db");

        ResponseEntity<ApiResponse<Object>> entity = handler.handleGenericException(secretException, request, response);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode());
        ApiResponse<Object> body = entity.getBody();
        assertNotNull(body);

        assertFalse(body.isSuccess());
        assertEquals("An unexpected error occurred. Please try again later.", body.getMessage());
        assertEquals("test-correlation-id-999", body.getCorrelationId());
        assertEquals("test-correlation-id-999", response.getHeader(CorrelationIdContext.CORRELATION_ID_HEADER));
        assertNull(body.getData());

        // STRICT SECURITY ASSERTIONS: Verify ZERO internal details leak in the client response
        String fullMessage = body.getMessage();
        assertFalse(fullMessage.contains("RuntimeException"), "Must not expose exception class name");
        assertFalse(fullMessage.contains("mongodb"), "Must not expose DB details");
        assertFalse(fullMessage.contains("secret"), "Must not expose sensitive words");
        assertFalse(fullMessage.contains(".java"), "Must not expose Java source files");
        assertFalse(fullMessage.contains("com.serviceonwheels"), "Must not expose package names");
        assertFalse(fullMessage.contains("\tat "), "Must not expose stack trace lines");
    }

    @Test
    void handleMessageNotReadable_returnsHttp400AndSafeMessageWithoutJacksonDetails() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Cannot deserialize value of type `com.example.InternalClass` from String \"bad\": not a valid representation",
                new MockHttpInputMessage(new byte[0])
        );

        ResponseEntity<ApiResponse<Object>> entity = handler.handleMessageNotReadable(ex, request, response);

        assertEquals(HttpStatus.BAD_REQUEST, entity.getStatusCode());
        ApiResponse<Object> body = entity.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Malformed JSON request or invalid request body format.", body.getMessage());
        assertFalse(body.getMessage().contains("InternalClass"));
        assertFalse(body.getMessage().contains("deserialize"));
        assertEquals("test-correlation-id-999", body.getCorrelationId());
    }

    @Test
    void handleValidationErrors_returnsHttp400WithCleanFieldErrors() throws NoSuchMethodException {
        TestTarget target = new TestTarget();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.addError(new FieldError("target", "email", "Email must be valid"));
        bindingResult.addError(new FieldError("target", "password", "Password is required"));

        Method method = getClass().getDeclaredMethod("dummyMethod", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiResponse<Object>> entity = handler.handleValidationErrors(ex, request, response);

        assertEquals(HttpStatus.BAD_REQUEST, entity.getStatusCode());
        ApiResponse<Object> body = entity.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Validation failed", body.getMessage());
        assertEquals("test-correlation-id-999", body.getCorrelationId());

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) body.getData();
        assertEquals("Email must be valid", errors.get("email"));
        assertEquals("Password is required", errors.get("password"));
    }

    @Test
    void handleInvalidCredentials_returnsHttp401WithGenericMessage() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Internal credential failure");

        ResponseEntity<ApiResponse<Object>> entity = handler.handleInvalidCredentials(ex, request, response);

        assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
        ApiResponse<Object> body = entity.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Invalid email or password. Please try again.", body.getMessage());
        assertEquals("test-correlation-id-999", body.getCorrelationId());
    }

    @Test
    void handleBadCredentials_returnsHttp401WithGenericMessage() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        ResponseEntity<ApiResponse<Object>> entity = handler.handleInvalidCredentials(ex, request, response);

        assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
        ApiResponse<Object> body = entity.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Invalid email or password. Please try again.", body.getMessage());
    }

    @Test
    void handleAccessDenied_returnsHttp403WithGenericMessage() {
        AccessDeniedException ex = new AccessDeniedException("Access is denied");

        ResponseEntity<ApiResponse<Object>> entity = handler.handleAccessDenied(ex, request, response);

        assertEquals(HttpStatus.FORBIDDEN, entity.getStatusCode());
        ApiResponse<Object> body = entity.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Access denied. You do not have permission to access this resource.", body.getMessage());
    }

    @Test
    void handleResourceNotFoundExceptions_returnHttp404() {
        UserNotFoundException userEx = new UserNotFoundException("User not found.");
        ResponseEntity<ApiResponse<Object>> userRes = handler.handleUserNotFound(userEx, request, response);
        assertEquals(HttpStatus.NOT_FOUND, userRes.getStatusCode());
        assertEquals("User not found.", userRes.getBody().getMessage());

        ServiceRequestNotFoundException reqEx = new ServiceRequestNotFoundException("Service request not found.");
        ResponseEntity<ApiResponse<Object>> reqRes = handler.handleServiceRequestNotFound(reqEx, request, response);
        assertEquals(HttpStatus.NOT_FOUND, reqRes.getStatusCode());
        assertEquals("Service request not found.", reqRes.getBody().getMessage());

        MechanicNotFoundException mechEx = new MechanicNotFoundException("Mechanic not found.");
        ResponseEntity<ApiResponse<Object>> mechRes = handler.handleMechanicNotFound(mechEx, request, response);
        assertEquals(HttpStatus.NOT_FOUND, mechRes.getStatusCode());
        assertEquals("Mechanic not found.", mechRes.getBody().getMessage());
    }

    @Test
    void handleNoResourceFoundException_returnsHttp404() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/unknown/path");

        ResponseEntity<ApiResponse<Object>> entity = handler.handleNoResourceFound(ex, request, response);

        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
        assertEquals("The requested resource was not found.", entity.getBody().getMessage());
    }

    @Test
    void handleMethodNotSupportedException_returnsHttp405() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

        ResponseEntity<ApiResponse<Object>> entity = handler.handleMethodNotSupported(ex, request, response);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, entity.getStatusCode());
        assertTrue(entity.getBody().getMessage().contains("DELETE"));
    }

    @Test
    void handleEmailDeliveryException_returnsHttp503AndSanitizedMessage() {
        EmailDeliveryException ex = new EmailDeliveryException("Failed SMTP auth to smtp.gmail.com:587 with password XYZ");

        ResponseEntity<ApiResponse<Object>> entity = handler.handleEmailDelivery(ex, request, response);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, entity.getStatusCode());
        ApiResponse<Object> body = entity.getBody();
        assertNotNull(body);
        assertEquals("Unable to send email at this time. Please try again later.", body.getMessage());
        assertFalse(body.getMessage().contains("smtp.gmail.com"));
        assertFalse(body.getMessage().contains("XYZ"));
    }

    @SuppressWarnings("unused")
    private void dummyMethod(String param) {}

    private static class TestTarget {}
}
