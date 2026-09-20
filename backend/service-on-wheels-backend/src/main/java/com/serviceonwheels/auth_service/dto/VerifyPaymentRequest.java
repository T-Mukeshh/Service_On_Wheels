package com.serviceonwheels.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sent by the frontend after Razorpay checkout completes.
 * The backend verifies the signature to confirm payment authenticity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment verification request containing Razorpay checkout callback data")
public class VerifyPaymentRequest {

    @NotBlank(message = "Razorpay order ID is required")
    @Schema(description = "Razorpay order ID", example = "order_OeXXXXXXXXX")
    private String razorpayOrderId;

    @NotBlank(message = "Razorpay payment ID is required")
    @Schema(description = "Razorpay payment ID", example = "pay_OeXXXXXXXXX")
    private String razorpayPaymentId;

    @NotBlank(message = "Razorpay signature is required")
    @Schema(description = "HMAC SHA256 signature for verification")
    private String razorpaySignature;
}
