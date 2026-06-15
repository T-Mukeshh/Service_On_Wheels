package com.serviceonwheels.auth_service.service;

import com.serviceonwheels.auth_service.dto.ServiceRequestResponse;
import com.serviceonwheels.auth_service.exception.BadRequestException;
import com.serviceonwheels.auth_service.exception.ForbiddenException;
import com.serviceonwheels.auth_service.exception.ServiceRequestNotFoundException;
import com.serviceonwheels.auth_service.exception.UserNotFoundException;
import com.serviceonwheels.auth_service.model.Mechanic;
import com.serviceonwheels.auth_service.model.RequestStatus;
import com.serviceonwheels.auth_service.model.ServiceRequest;
import com.serviceonwheels.auth_service.repository.ServiceRequestRepository;
import com.serviceonwheels.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Handles request lifecycle transitions with validation.
 * Think: Uber Driver workflow — each status transition is validated
 * and the appropriate audit timestamp is recorded.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestLifecycleService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final MechanicService mechanicService;
    private final UserRepository userRepository;

    /**
     * Accept a request: PENDING → ASSIGNED.
     * Auto-assigns an available mechanic.
     */
    public ServiceRequestResponse acceptRequest(String requestId) {
        ServiceRequest request = findRequest(requestId);
        validateTransition(request, RequestStatus.PENDING, RequestStatus.ASSIGNED);

        // Find an available mechanic
        List<Mechanic> available = mechanicService.findAvailable();
        if (available.isEmpty()) {
            throw new BadRequestException("No mechanic available at this time. Please try again later.");
        }

        // Pick first available (simple, deterministic)
        Mechanic mechanic = available.get(0);

        // Assign mechanic to request
        mechanicService.assignToRequest(mechanic.getId(), requestId);

        // Update request with mechanic info
        request.setStatus(RequestStatus.ASSIGNED);
        request.setAssignedMechanicId(mechanic.getId());
        request.setMechanicName(mechanic.getName());
        request.setMechanicPhone(mechanic.getPhone());
        request.setMechanicVehicle(mechanic.getVehicle());
        request.setMechanicRating(mechanic.getRating());
        request.setMechanicStartLat(
                mechanic.getCurrentLat() != null ? mechanic.getCurrentLat()
                        : TrackingService.startLat(request.getLatitude()));
        request.setMechanicStartLng(
                mechanic.getCurrentLng() != null ? mechanic.getCurrentLng()
                        : TrackingService.startLng(request.getLongitude()));
        request.setAssignedAt(LocalDateTime.now());

        log.info("Request [{}] assigned to mechanic [{}] ({})", requestId, mechanic.getId(), mechanic.getName());
        return toResponse(serviceRequestRepository.save(request));
    }

    /**
     * Start trip: ASSIGNED → ON_THE_WAY.
     */
    public ServiceRequestResponse startTrip(String requestId) {
        ServiceRequest request = findRequest(requestId);
        validateTransition(request, RequestStatus.ASSIGNED, RequestStatus.ON_THE_WAY);

        request.setStatus(RequestStatus.ON_THE_WAY);
        log.info("Request [{}] status → ON_THE_WAY", requestId);
        return toResponse(serviceRequestRepository.save(request));
    }

    /**
     * Mark arrived: ON_THE_WAY → ARRIVED.
     */
    public ServiceRequestResponse markArrived(String requestId) {
        ServiceRequest request = findRequest(requestId);
        validateTransition(request, RequestStatus.ON_THE_WAY, RequestStatus.ARRIVED);

        request.setStatus(RequestStatus.ARRIVED);
        request.setArrivedAt(LocalDateTime.now());
        log.info("Request [{}] status → ARRIVED", requestId);
        return toResponse(serviceRequestRepository.save(request));
    }

    /**
     * Begin service: ARRIVED → IN_SERVICE.
     */
    public ServiceRequestResponse beginService(String requestId) {
        ServiceRequest request = findRequest(requestId);
        validateTransition(request, RequestStatus.ARRIVED, RequestStatus.IN_SERVICE);

        request.setStatus(RequestStatus.IN_SERVICE);
        request.setServiceStartedAt(LocalDateTime.now());
        log.info("Request [{}] status → IN_SERVICE", requestId);
        return toResponse(serviceRequestRepository.save(request));
    }

    /**
     * Complete service: IN_SERVICE → COMPLETED.
     * Releases the assigned mechanic back to AVAILABLE.
     */
    public ServiceRequestResponse completeService(String requestId) {
        ServiceRequest request = findRequest(requestId);
        validateTransition(request, RequestStatus.IN_SERVICE, RequestStatus.COMPLETED);

        request.setStatus(RequestStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());

        // Release the mechanic
        if (request.getAssignedMechanicId() != null) {
            mechanicService.releaseFromRequest(request.getAssignedMechanicId());
        }

        log.info("Request [{}] status → COMPLETED", requestId);
        return toResponse(serviceRequestRepository.save(request));
    }

    /**
     * Cancel request: any non-COMPLETED status → CANCELLED.
     */
    public ServiceRequestResponse cancelRequest(String requestId) {
        ServiceRequest request = findRequest(requestId);
        return cancelRequest(request);
    }

    public ServiceRequestResponse cancelOwnedRequest(String requestId, String email) {
        ServiceRequest request = findRequest(requestId);
        String userId = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found."))
                .getId();

        if (!userId.equals(request.getUserId())) {
            throw new ForbiddenException("You can only cancel your own service requests.");
        }

        return cancelRequest(request);
    }

    private ServiceRequestResponse cancelRequest(ServiceRequest request) {
        String requestId = request.getId();

        if (request.getStatus() == RequestStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel an already completed request.");
        }
        if (request.getStatus() == RequestStatus.CANCELLED) {
            throw new IllegalStateException("Request is already cancelled.");
        }

        request.setStatus(RequestStatus.CANCELLED);
        request.setCancelledAt(LocalDateTime.now());

        // Release the mechanic if assigned
        if (request.getAssignedMechanicId() != null) {
            try {
                mechanicService.releaseFromRequest(request.getAssignedMechanicId());
            } catch (Exception e) {
                log.warn("Could not release mechanic [{}] during cancellation: {}",
                        request.getAssignedMechanicId(), e.getMessage());
            }
        }

        log.info("Request [{}] status → CANCELLED", requestId);
        return toResponse(serviceRequestRepository.save(request));
    }

    // ── Helpers ──────────────────────────────────────────

    private ServiceRequest findRequest(String requestId) {
        return serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new ServiceRequestNotFoundException("Service request not found: " + requestId));
    }

    private void validateTransition(ServiceRequest request, RequestStatus expectedFrom, RequestStatus to) {
        if (request.getStatus() != expectedFrom) {
            throw new IllegalStateException(
                    String.format("Invalid status transition: cannot move from %s to %s (expected current status: %s)",
                            request.getStatus(), to, expectedFrom));
        }
    }

    private ServiceRequestResponse toResponse(ServiceRequest entity) {
        return ServiceRequestResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .vehicleType(entity.getVehicleType())
                .vehicleNumber(entity.getVehicleNumber())
                .problemDescription(entity.getProblemDescription())
                .selectedIssue(entity.getSelectedIssue())
                .additionalNotes(entity.getAdditionalNotes())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .address(entity.getAddress())
                .status(entity.getStatus())
                .assignedMechanicId(entity.getAssignedMechanicId())
                .createdAt(entity.getCreatedAt())
                // Phase 4 fields
                .mechanicName(entity.getMechanicName())
                .mechanicPhone(entity.getMechanicPhone())
                .mechanicVehicle(entity.getMechanicVehicle())
                .mechanicRating(entity.getMechanicRating())
                .assignedAt(entity.getAssignedAt())
                .arrivedAt(entity.getArrivedAt())
                .serviceStartedAt(entity.getServiceStartedAt())
                .completedAt(entity.getCompletedAt())
                .cancelledAt(entity.getCancelledAt())
                .build();
    }
}
