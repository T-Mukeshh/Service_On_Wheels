package com.serviceonwheels.auth_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Represents a payment transaction linked to a completed service request.
 * Stores Razorpay order/payment IDs and the HMAC verification signature.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
public class Payment {

    @Id
    private String id;

    @Indexed(unique = true)
    private String serviceRequestId;

    @Indexed
    private String userId;

    /** Amount in the smallest currency unit (paise for INR). */
    private Integer amount;

    private String currency;

    /** Razorpay-generated order ID (order_XXXXX). */
    @Indexed(unique = true)
    private String razorpayOrderId;

    /** Razorpay-generated payment ID (pay_XXXXX), set after checkout. */
    private String razorpayPaymentId;

    /** HMAC-SHA256 signature returned by Razorpay checkout, used for verification. */
    private String razorpaySignature;

    private PaymentStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;
}
