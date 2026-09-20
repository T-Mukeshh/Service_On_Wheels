package com.serviceonwheels.auth_service.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.serviceonwheels.auth_service.dto.CreateOrderResponse;
import com.serviceonwheels.auth_service.dto.VerifyPaymentRequest;
import com.serviceonwheels.auth_service.exception.BadRequestException;
import com.serviceonwheels.auth_service.model.Payment;
import com.serviceonwheels.auth_service.model.PaymentStatus;
import com.serviceonwheels.auth_service.model.RequestStatus;
import com.serviceonwheels.auth_service.model.ServiceRequest;
import com.serviceonwheels.auth_service.model.User;
import com.serviceonwheels.auth_service.repository.PaymentRepository;
import com.serviceonwheels.auth_service.repository.ServiceRequestRepository;
import com.serviceonwheels.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private UserRepository userRepository;

    @Mock
    private RazorpayClient razorpayClient;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "razorpayClient", razorpayClient);
        ReflectionTestUtils.setField(paymentService, "razorpayKeyId", "testKey");
        ReflectionTestUtils.setField(paymentService, "razorpayKeySecret", "testSecret");
    }

    @Test
    void createOrder_shouldThrowIfNotCompleted() {
        User user = new User();
        user.setId("user1");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        ServiceRequest request = new ServiceRequest();
        request.setUserId("user1");
        request.setStatus(RequestStatus.IN_PROGRESS);
        when(serviceRequestRepository.findById("req1")).thenReturn(Optional.of(request));

        assertThrows(BadRequestException.class, () -> paymentService.createOrder("req1", "test@example.com"));
    }
}
