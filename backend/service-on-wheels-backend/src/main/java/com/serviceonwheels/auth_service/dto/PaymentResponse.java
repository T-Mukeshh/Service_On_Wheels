package com.serviceonwheels.auth_service.dto;

import com.serviceonwheels.auth_service.model.PaymentStatus;
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
@Schema(description = "Payment details response")
public class PaymentResponse {

    @Schema(description = "Payment record ID")
    private String id;

    @Schema(description = "Associated service request ID")
    private String serviceRequestId;

    @Schema(description = "Amount in smallest currency unit (paise)", example = "50000")
    private Integer amount;

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @Schema(description = "Payment status")
    private PaymentStatus status;

    @Schema(description = "Razorpay order ID")
    private String razorpayOrderId;

    @Schema(description = "Razorpay payment ID (set after successful checkout)")
    private String razorpayPaymentId;

    @Schema(description = "Timestamp when payment was created")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when payment was captured")
    private LocalDateTime paidAt;
}
