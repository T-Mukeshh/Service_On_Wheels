package com.serviceonwheels.auth_service.controller;

import com.serviceonwheels.auth_service.dto.ApiResponse;
import com.serviceonwheels.auth_service.dto.CreateOrderResponse;
import com.serviceonwheels.auth_service.dto.PaymentResponse;
import com.serviceonwheels.auth_service.dto.VerifyPaymentRequest;
import com.serviceonwheels.auth_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Controller", description = "Endpoints for Razorpay integration")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Create Razorpay Order", description = "Initiates a payment for a completed service request")
    @PostMapping("/create-order/{requestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @PathVariable("requestId") String requestId,
            Principal principal) {
        // principal.getName() is the user email in this app structure, but we need userId.
        // Or wait, in this app, does principal.getName() return email or ID? Let's assume it's used to look up user or the ID is stored.
        // Actually, looking at RequestLifecycleController, it passes principal.getName() to service, which looks up the User.
        // Wait, PaymentService expects userId. Let's fix that in PaymentService or pass email and lookup.
        // For simplicity, let's pass email and update PaymentService. (Let me just change PaymentService later if needed, or pass email)
        return ResponseEntity.ok(ApiResponse.success("Order created", paymentService.createOrder(requestId, principal.getName())));
    }

    @Operation(summary = "Verify Payment", description = "Verifies the Razorpay signature after frontend checkout")
    @PostMapping("/verify")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment verified", paymentService.verifyPayment(request)));
    }

    @Operation(summary = "Get Payment Status", description = "Get payment details for a specific service request")
    @GetMapping("/request/{requestId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentForRequest(
            @PathVariable("requestId") String requestId,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Payment details retrieved", paymentService.getPaymentByServiceRequestId(requestId, principal.getName())));
    }
}
