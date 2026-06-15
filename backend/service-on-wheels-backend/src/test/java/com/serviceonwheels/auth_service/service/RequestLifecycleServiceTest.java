package com.serviceonwheels.auth_service.service;

import com.serviceonwheels.auth_service.dto.ServiceRequestResponse;
import com.serviceonwheels.auth_service.exception.BadRequestException;
import com.serviceonwheels.auth_service.exception.ForbiddenException;
import com.serviceonwheels.auth_service.exception.ServiceRequestNotFoundException;
import com.serviceonwheels.auth_service.model.Mechanic;
import com.serviceonwheels.auth_service.model.MechanicStatus;
import com.serviceonwheels.auth_service.model.RequestStatus;
import com.serviceonwheels.auth_service.model.ServiceRequest;
import com.serviceonwheels.auth_service.model.User;
import com.serviceonwheels.auth_service.repository.ServiceRequestRepository;
import com.serviceonwheels.auth_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestLifecycleServiceTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private MechanicService mechanicService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RequestLifecycleService requestLifecycleService;

    private ServiceRequest serviceRequest;
    private Mechanic mechanic;

    @BeforeEach
    void setUp() {
        serviceRequest = ServiceRequest.builder()
                .id("req-1")
                .userId("user-1")
                .status(RequestStatus.PENDING)
                .latitude(12.34)
                .longitude(56.78)
                .build();

        mechanic = Mechanic.builder()
                .id("mech-1")
                .name("Test Mechanic")
                .status(MechanicStatus.AVAILABLE)
                .build();
    }

    @Test
    void acceptRequest_assignsMechanic_setsStatusAssigned() {
        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(mechanicService.findAvailable()).thenReturn(List.of(mechanic));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(i -> i.getArgument(0));

        ServiceRequestResponse response = requestLifecycleService.acceptRequest("req-1");

        assertEquals(RequestStatus.ASSIGNED, response.getStatus());
        assertEquals("mech-1", response.getAssignedMechanicId());
        assertEquals("Test Mechanic", response.getMechanicName());
        assertNotNull(response.getAssignedAt());
        verify(mechanicService).assignToRequest("mech-1", "req-1");
    }

    @Test
    void acceptRequest_noMechanicAvailable_throwsBadRequest() {
        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(mechanicService.findAvailable()).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> requestLifecycleService.acceptRequest("req-1"));
        verify(mechanicService, never()).assignToRequest(anyString(), anyString());
    }

    @Test
    void startTrip_fromAssigned_setsOnTheWay() {
        serviceRequest.setStatus(RequestStatus.ASSIGNED);
        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(i -> i.getArgument(0));

        ServiceRequestResponse response = requestLifecycleService.startTrip("req-1");

        assertEquals(RequestStatus.ON_THE_WAY, response.getStatus());
    }

    @Test
    void startTrip_fromWrongStatus_throwsIllegalState() {
        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest)); // Status is PENDING

        assertThrows(IllegalStateException.class, () -> requestLifecycleService.startTrip("req-1"));
    }

    @Test
    void markArrived_fromOnTheWay_setsArrived() {
        serviceRequest.setStatus(RequestStatus.ON_THE_WAY);
        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(i -> i.getArgument(0));

        ServiceRequestResponse response = requestLifecycleService.markArrived("req-1");

        assertEquals(RequestStatus.ARRIVED, response.getStatus());
        assertNotNull(response.getArrivedAt());
    }

    @Test
    void beginService_fromArrived_setsInService() {
        serviceRequest.setStatus(RequestStatus.ARRIVED);
        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(i -> i.getArgument(0));

        ServiceRequestResponse response = requestLifecycleService.beginService("req-1");

        assertEquals(RequestStatus.IN_SERVICE, response.getStatus());
        assertNotNull(response.getServiceStartedAt());
    }

    @Test
    void completeService_fromInService_setsCompleted_releasesMechanic() {
        serviceRequest.setStatus(RequestStatus.IN_SERVICE);
        serviceRequest.setAssignedMechanicId("mech-1");
        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(i -> i.getArgument(0));

        ServiceRequestResponse response = requestLifecycleService.completeService("req-1");

        assertEquals(RequestStatus.COMPLETED, response.getStatus());
        assertNotNull(response.getCompletedAt());
        verify(mechanicService).releaseFromRequest("mech-1");
    }

    @Test
    void cancelRequest_fromAnyActive_setsCancelled_releasesMechanic() {
        serviceRequest.setStatus(RequestStatus.ON_THE_WAY);
        serviceRequest.setAssignedMechanicId("mech-1");
        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(i -> i.getArgument(0));

        ServiceRequestResponse response = requestLifecycleService.cancelRequest("req-1");

        assertEquals(RequestStatus.CANCELLED, response.getStatus());
        assertNotNull(response.getCancelledAt());
        verify(mechanicService).releaseFromRequest("mech-1");
    }

    @Test
    void cancelRequest_fromCompleted_throwsIllegalState() {
        serviceRequest.setStatus(RequestStatus.COMPLETED);
        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));

        assertThrows(IllegalStateException.class, () -> requestLifecycleService.cancelRequest("req-1"));
        verify(mechanicService, never()).releaseFromRequest(anyString());
    }

    @Test
    void cancelOwnedRequest_rejectsDifferentUser() {
        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(userRepository.findByEmail("attacker@example.com"))
                .thenReturn(Optional.of(User.builder().id("user-2").build()));

        assertThrows(
                ForbiddenException.class,
                () -> requestLifecycleService.cancelOwnedRequest("req-1", "attacker@example.com"));

        verify(serviceRequestRepository, never()).save(any());
    }

    @Test
    void cancelOwnedRequest_allowsOwner() {
        when(serviceRequestRepository.findById("req-1")).thenReturn(Optional.of(serviceRequest));
        when(userRepository.findByEmail("owner@example.com"))
                .thenReturn(Optional.of(User.builder().id("user-1").build()));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(i -> i.getArgument(0));

        ServiceRequestResponse response =
                requestLifecycleService.cancelOwnedRequest("req-1", "owner@example.com");

        assertEquals(RequestStatus.CANCELLED, response.getStatus());
    }
}
