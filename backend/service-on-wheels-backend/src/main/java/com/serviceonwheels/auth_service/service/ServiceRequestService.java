package com.serviceonwheels.auth_service.service;

import com.serviceonwheels.auth_service.dto.CreateServiceRequest;
import com.serviceonwheels.auth_service.dto.ServiceRequestResponse;
import com.serviceonwheels.auth_service.exception.UserNotFoundException;
import com.serviceonwheels.auth_service.model.RequestStatus;
import com.serviceonwheels.auth_service.model.ServiceRequest;
import com.serviceonwheels.auth_service.repository.ServiceRequestRepository;
import com.serviceonwheels.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceRequestService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final RequestLifecycleService requestLifecycleService;

    public ServiceRequestResponse create(CreateServiceRequest dto, String email) {
        String userId = resolveUserId(email);
        ServiceRequest entity = ServiceRequest.builder()
                .userId(userId)
                .vehicleType(dto.getVehicleType().trim())
                .vehicleNumber(dto.getVehicleNumber().trim())
                .problemDescription(dto.getProblemDescription().trim())
                .selectedIssue(dto.getSelectedIssue() != null ? dto.getSelectedIssue().trim() : null)
                .additionalNotes(dto.getAdditionalNotes() != null ? dto.getAdditionalNotes().trim() : null)
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .address(dto.getAddress().trim())
                .status(RequestStatus.PENDING)
                .assignedMechanicId(null)
                .trackingStatus(null)
                .build();
        ServiceRequest saved = serviceRequestRepository.save(entity);
        log.info("Service request created: id={}, userId={}", saved.getId(), userId);

        // Auto-assign a mechanic via lifecycle service
        try {
            return requestLifecycleService.acceptRequest(saved.getId());
        } catch (Exception e) {
            log.warn("Auto-assignment failed for request [{}]: {}. Request remains PENDING.",
                    saved.getId(), e.getMessage());
            return toResponse(saved);
        }
    }

    public List<ServiceRequestResponse> listMine(String email) {
        String userId = resolveUserId(email);
        return serviceRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private String resolveUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found."))
                .getId();
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

