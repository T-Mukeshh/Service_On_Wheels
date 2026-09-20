package com.serviceonwheels.auth_service.repository;

import com.serviceonwheels.auth_service.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PaymentRepository extends MongoRepository<Payment, String> {

    Optional<Payment> findByServiceRequestId(String serviceRequestId);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}
