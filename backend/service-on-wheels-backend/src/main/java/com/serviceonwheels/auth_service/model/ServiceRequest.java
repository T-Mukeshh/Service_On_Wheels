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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "service_requests")
public class ServiceRequest {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String vehicleType;

    private String vehicleNumber;

    private String problemDescription;

    private String selectedIssue;

    private String additionalNotes;

    private Double latitude;

    private Double longitude;

    private String address;

    private RequestStatus status;

    private String assignedMechanicId;

    // ── Tracking fields ──────────────────────────────────
    private TrackingStatus trackingStatus;

    private String mechanicName;

    private String mechanicPhone;

    private String mechanicVehicle;

    private Double mechanicRating;

    /** Mechanic's starting latitude (simulation origin). */
    private Double mechanicStartLat;

    /** Mechanic's starting longitude (simulation origin). */
    private Double mechanicStartLng;

    /** Timestamp when mechanic was assigned. Used to compute simulated travel progress. */
    private LocalDateTime assignedAt;

    // ── Phase 4 audit timestamps ─────────────────────────
    private LocalDateTime arrivedAt;

    private LocalDateTime serviceStartedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private PaymentStatus paymentStatus;

    @CreatedDate
    private LocalDateTime createdAt;
}
