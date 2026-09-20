package com.serviceonwheels.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Returned after creating a Razorpay order.
 * The frontend uses these fields to open the Razorpay checkout modal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response after creating a Razorpay order for payment")
public class CreateOrderResponse {

    @Schema(description = "Razorpay order ID", example = "order_OeXXXXXXXXX")
    private String orderId;

    @Schema(description = "Amount in smallest currency unit (paise)", example = "50000")
    private Integer amount;

    @Schema(description = "Currency code", example = "INR")
    private String currency;

    @Schema(description = "Razorpay Key ID (public key for frontend)", example = "rzp_test_XXXXXX")
    private String keyId;

    @Schema(description = "Associated service request ID")
    private String serviceRequestId;
}
