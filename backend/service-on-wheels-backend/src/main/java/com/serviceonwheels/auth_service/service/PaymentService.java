package com.serviceonwheels.auth_service.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.serviceonwheels.auth_service.dto.CreateOrderResponse;
import com.serviceonwheels.auth_service.dto.PaymentResponse;
import com.serviceonwheels.auth_service.dto.VerifyPaymentRequest;
import com.serviceonwheels.auth_service.exception.BadRequestException;
import com.serviceonwheels.auth_service.exception.PaymentNotFoundException;
import com.serviceonwheels.auth_service.exception.ServiceRequestNotFoundException;
import com.serviceonwheels.auth_service.model.Payment;
import com.serviceonwheels.auth_service.model.PaymentStatus;
import com.serviceonwheels.auth_service.model.RequestStatus;
import com.serviceonwheels.auth_service.model.ServiceRequest;
import com.serviceonwheels.auth_service.repository.PaymentRepository;
import com.serviceonwheels.auth_service.repository.ServiceRequestRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final com.serviceonwheels.auth_service.repository.UserRepository userRepository;

    @Value("${razorpay.key.id:}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:}")
    private String razorpayKeySecret;

    private RazorpayClient razorpayClient;

    // Simulated pricing table (paise)
    private static final Map<String, Integer> PRICING_TABLE = Map.of(
            "Flat Tire", 50000,
            "Battery Jumpstart", 40000,
            "Engine Heating", 150000,
            "Empty Fuel", 30000,
            "Key Locked", 25000,
            "Other", 80000
    );

    @PostConstruct
    public void init() {
        if (razorpayKeyId != null && !razorpayKeyId.isEmpty() && razorpayKeySecret != null && !razorpayKeySecret.isEmpty()) {
            try {
                this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
                log.info("RazorpayClient initialized successfully.");
            } catch (RazorpayException e) {
                log.error("Failed to initialize RazorpayClient: {}", e.getMessage());
            }
        } else {
            log.warn("Razorpay credentials not found in environment. Payment service will not function.");
        }
    }

    public CreateOrderResponse createOrder(String serviceRequestId, String userEmail) {
        String userId = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new com.serviceonwheels.auth_service.exception.UserNotFoundException("User not found."))
                .getId();

        ServiceRequest request = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ServiceRequestNotFoundException("Service request not found"));

        if (!request.getUserId().equals(userId)) {
            throw new BadRequestException("You are not authorized to pay for this request.");
        }

        if (request.getStatus() != RequestStatus.COMPLETED) {
            throw new BadRequestException("Can only pay for COMPLETED service requests.");
        }

        // Check if payment already exists
        Optional<Payment> existingPaymentOpt = paymentRepository.findByServiceRequestId(serviceRequestId);
        if (existingPaymentOpt.isPresent()) {
            Payment existingPayment = existingPaymentOpt.get();
            if (existingPayment.getStatus() == PaymentStatus.CAPTURED) {
                throw new BadRequestException("Payment has already been captured for this request.");
            }
            // If an order already exists but isn't paid, we can return it
            if (existingPayment.getStatus() == PaymentStatus.CREATED) {
                return buildOrderResponse(existingPayment);
            }
        }

        Integer amount = PRICING_TABLE.getOrDefault(request.getSelectedIssue(), 80000);

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "rcpt_" + serviceRequestId);

            if (razorpayClient == null) {
                throw new BadRequestException("Razorpay client not configured.");
            }

            Order order = razorpayClient.orders.create(orderRequest);

            Payment payment = Payment.builder()
                    .serviceRequestId(serviceRequestId)
                    .userId(userId)
                    .amount(amount)
                    .currency("INR")
                    .razorpayOrderId(order.get("id"))
                    .status(PaymentStatus.CREATED)
                    .build();

            payment = paymentRepository.save(payment);
            
            return buildOrderResponse(payment);

        } catch (RazorpayException e) {
            log.error("Error creating Razorpay order: ", e);
            throw new BadRequestException("Could not initiate payment: " + e.getMessage());
        }
    }

    public PaymentResponse verifyPayment(VerifyPaymentRequest verifyRequest) {
        Payment payment = paymentRepository.findByRazorpayOrderId(verifyRequest.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment record not found for order: " + verifyRequest.getRazorpayOrderId()));

        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return toResponse(payment); // Already verified
        }

        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", verifyRequest.getRazorpayOrderId());
            options.put("razorpay_payment_id", verifyRequest.getRazorpayPaymentId());
            options.put("razorpay_signature", verifyRequest.getRazorpaySignature());

            if (razorpayKeySecret == null || razorpayKeySecret.isEmpty()) {
                throw new BadRequestException("Razorpay secret not configured.");
            }

            boolean isValid = Utils.verifyPaymentSignature(options, razorpayKeySecret);

            if (isValid) {
                payment.setRazorpayPaymentId(verifyRequest.getRazorpayPaymentId());
                payment.setRazorpaySignature(verifyRequest.getRazorpaySignature());
                payment.setStatus(PaymentStatus.CAPTURED);
                payment.setPaidAt(LocalDateTime.now());
                payment = paymentRepository.save(payment);
                
                log.info("Payment verified successfully for order: {}", payment.getRazorpayOrderId());
                return toResponse(payment);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                log.warn("Payment signature verification failed for order: {}", payment.getRazorpayOrderId());
                throw new BadRequestException("Payment verification failed. Invalid signature.");
            }
        } catch (RazorpayException e) {
             log.error("Error verifying payment signature: ", e);
             throw new BadRequestException("Error verifying payment: " + e.getMessage());
        }
    }

    public PaymentResponse getPaymentByServiceRequestId(String serviceRequestId, String userEmail) {
         String userId = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new com.serviceonwheels.auth_service.exception.UserNotFoundException("User not found."))
                .getId();

         Payment payment = paymentRepository.findByServiceRequestId(serviceRequestId)
                .orElseThrow(() -> new PaymentNotFoundException("No payment found for service request: " + serviceRequestId));
         
         if (!payment.getUserId().equals(userId)) {
             throw new BadRequestException("Unauthorized access to payment record.");
         }
         return toResponse(payment);
    }

    private CreateOrderResponse buildOrderResponse(Payment payment) {
        return CreateOrderResponse.builder()
                .orderId(payment.getRazorpayOrderId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .keyId(razorpayKeyId)
                .serviceRequestId(payment.getServiceRequestId())
                .build();
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .serviceRequestId(payment.getServiceRequestId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .createdAt(payment.getCreatedAt())
                .paidAt(payment.getPaidAt())
                .build();
    }
}
