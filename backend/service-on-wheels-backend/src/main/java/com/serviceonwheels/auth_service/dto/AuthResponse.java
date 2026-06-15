package com.serviceonwheels.auth_service.dto;

import com.serviceonwheels.auth_service.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object containing JWT token and user details after successful authentication")
public class AuthResponse {

    @Schema(description = "JWT Bearer token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;

    @Schema(description = "User's unique identifier", example = "65a1b2c3d4e5f6g7h8i9j0k")
    private String userId;

    @Schema(description = "User's full name", example = "John Doe")
    private String name;

    @Schema(description = "User's email address", example = "john.doe@example.com")
    private String email;

    @Schema(description = "User's role (USER or ADMIN)", example = "USER")
    private Role role;

    @Schema(description = "Additional message", example = "Login successful")
    private String message;
}
