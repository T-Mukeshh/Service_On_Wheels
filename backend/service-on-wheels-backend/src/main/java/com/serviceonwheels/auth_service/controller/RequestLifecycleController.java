package com.serviceonwheels.auth_service.controller;

import com.serviceonwheels.auth_service.dto.ApiResponse;
import com.serviceonwheels.auth_service.dto.ServiceRequestResponse;
import com.serviceonwheels.auth_service.service.RequestLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * REST controller for request lifecycle actions.
 * Maps each mechanic workflow step to a dedicated endpoint.
 *
 * <p>Think: Uber driver actions — accept, start trip, arrived,
 * begin service, complete, cancel.</p>
 */
@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Tag(name = "Request Lifecycle Controller", description = "Endpoints for managing the lifecycle of a service request")
public class RequestLifecycleController {

    private final RequestLifecycleService lifecycleService;

    @Operation(summary = "Accept Request", description = "Mechanic accepts a service request")
    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> accept(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success("Request accepted", lifecycleService.acceptRequest(id)));
    }

    @Operation(summary = "Start Trip", description = "Mechanic starts trip to the user's location")
    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> startTrip(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success("Trip started", lifecycleService.startTrip(id)));
    }

    @Operation(summary = "Mark Arrived", description = "Mechanic arrives at the location")
    @PutMapping("/{id}/arrived")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> arrived(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success("Mechanic arrived", lifecycleService.markArrived(id)));
    }

    @Operation(summary = "Begin Service", description = "Mechanic starts the actual service/repair")
    @PutMapping("/{id}/service-start")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> serviceStart(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success("Service started", lifecycleService.beginService(id)));
    }

    @Operation(summary = "Complete Service", description = "Mechanic successfully completes the service")
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> complete(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success("Service completed", lifecycleService.completeService(id)));
    }

    @Operation(summary = "Cancel Request", description = "Customer cancels their service request")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> cancel(
            @PathVariable("id") String id,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.success("Request cancelled", lifecycleService.cancelOwnedRequest(id, principal.getName())));
    }
}
