package com.serviceonwheels.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request object for resetting a user's password using a token")
public class ResetPasswordRequest {

    @Schema(description = "Password reset token sent via email", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    @NotBlank(message = "Token is required")
    private String token;

    @Schema(description = "The new password", example = "newSecretPassword!123")
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;
}
