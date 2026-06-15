package com.serviceonwheels.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request object for initiating a password reset")
public class ForgotPasswordRequest {

    @Schema(description = "User's registered email address", example = "john.doe@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "A valid email address is required")
    private String email;
}
