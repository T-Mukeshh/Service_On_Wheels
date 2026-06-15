package com.serviceonwheels.auth_service.dto;

import com.serviceonwheels.auth_service.model.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object containing service request details")
public class ServiceRequestResponse {

    @Schema(description = "Service request ID", example = "req-123")
    private String id;
    
    @Schema(description = "ID of the user who created the request", example = "user-456")
    private String userId;
    
    @Schema(description = "Type of vehicle", example = "Car")
    private String vehicleType;
    
    @Schema(description = "Vehicle registration number", example = "KA 01 AB 1234")
    private String vehicleNumber;
    
    @Schema(description = "Problem description", example = "Engine heating up")
    private String problemDescription;
    
    @Schema(description = "Selected issue category", example = "Engine Heating")
    private String selectedIssue;
    
    @Schema(description = "Additional notes", example = "Please bring extra coolant")
    private String additionalNotes;
    
    @Schema(description = "Latitude of the user's location", example = "12.9716")
    private Double latitude;
    
    @Schema(description = "Longitude of the user's location", example = "77.5946")
    private Double longitude;
    
    @Schema(description = "Address of the user's location", example = "MG Road, Bangalore")
    private String address;
    
    @Schema(description = "Current status of the request", example = "ASSIGNED")
    private RequestStatus status;
    
    @Schema(description = "ID of the assigned mechanic, if any", example = "mech-789")
    private String assignedMechanicId;
    
    @Schema(description = "Timestamp when the request was created")
    private LocalDateTime createdAt;

    // ── Mechanic details (Phase 4) ───────────────────────
    @Schema(description = "Name of the assigned mechanic")
    private String mechanicName;
    
    @Schema(description = "Phone number of the assigned mechanic")
    private String mechanicPhone;
    
    @Schema(description = "Vehicle of the assigned mechanic")
    private String mechanicVehicle;
    
    @Schema(description = "Rating of the assigned mechanic")
    private Double mechanicRating;

    // ── Audit timestamps (Phase 4) ───────────────────────
    @Schema(description = "Timestamp when the request was assigned to a mechanic")
    private LocalDateTime assignedAt;
    
    @Schema(description = "Timestamp when the mechanic arrived at the location")
    private LocalDateTime arrivedAt;
    
    @Schema(description = "Timestamp when the mechanic started the service")
    private LocalDateTime serviceStartedAt;
    
    @Schema(description = "Timestamp when the service was completed")
    private LocalDateTime completedAt;
    
    @Schema(description = "Timestamp when the request was cancelled")
    private LocalDateTime cancelledAt;
}
