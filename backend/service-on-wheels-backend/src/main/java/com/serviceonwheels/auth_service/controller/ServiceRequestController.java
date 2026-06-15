package com.serviceonwheels.auth_service.controller;

import com.serviceonwheels.auth_service.dto.ApiResponse;
import com.serviceonwheels.auth_service.dto.CreateServiceRequest;
import com.serviceonwheels.auth_service.dto.ServiceRequestResponse;
import com.serviceonwheels.auth_service.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/service")
@RequiredArgsConstructor
@Tag(name = "Service Request Controller", description = "Endpoints for customers to create and manage service requests")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    @Operation(summary = "Create Service Request", description = "Customer creates a new service request")
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> create(
            @Valid @RequestBody CreateServiceRequest body,
            Principal principal) {
        ServiceRequestResponse response = serviceRequestService.create(body, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Service request created successfully", response));
    }

    @Operation(summary = "Get My Requests", description = "Retrieves all service requests for the logged-in customer")
    @GetMapping("/my-requests")
    public ResponseEntity<ApiResponse<List<ServiceRequestResponse>>> myRequests(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Service requests retrieved", serviceRequestService.listMine(principal.getName())));
    }
}
