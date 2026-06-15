package com.serviceonwheels.auth_service.controller;

import com.serviceonwheels.auth_service.dto.ApiResponse;
import com.serviceonwheels.auth_service.dto.AuthResponse;
import com.serviceonwheels.auth_service.dto.ForgotPasswordRequest;
import com.serviceonwheels.auth_service.dto.LoginRequest;
import com.serviceonwheels.auth_service.dto.RegisterRequest;
import com.serviceonwheels.auth_service.dto.ResetPasswordRequest;
import com.serviceonwheels.auth_service.service.AuthService;
import com.serviceonwheels.auth_service.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Controller", description = "Endpoints for user registration, login, and password management")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @Operation(summary = "Register a new user", description = "Creates a new customer or mechanic account")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/auth/register - email: {}", request.getEmail());
        AuthResponse response = authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @Operation(summary = "Login user", description = "Authenticates a user and returns a JWT token")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login - email: {}", request.getEmail());
        AuthResponse response = authService.loginUser(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @Operation(summary = "Forgot Password", description = "Sends a password reset link to the user's email")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        log.info("POST /api/auth/forgot-password - email: {}", request.getEmail());
        Map<String, String> response = passwordResetService.handleForgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset email sent", response));
    }

    @Operation(summary = "Reset Password", description = "Resets the user's password using a valid token")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("POST /api/auth/reset-password");
        Map<String, String> response = passwordResetService.handleResetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", response));
    }

    @Operation(summary = "Validate Reset Token", description = "Checks if a password reset token is valid")
    @GetMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<Map<String, String>>> validateResetToken(@RequestParam String token) {
        log.info("GET /api/auth/validate-reset-token");
        Map<String, String> response = passwordResetService.validateToken(token);
        return ResponseEntity.ok(ApiResponse.success("Token is valid", response));
    }
}
