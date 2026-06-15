package com.serviceonwheels.auth_service.dto;

import com.serviceonwheels.auth_service.model.TrackingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for the GET /api/tracking/{requestId} endpoint.
 * Contains all data needed to render the Swiggy/Uber-style tracking page.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object containing real-time tracking data for a service request")
public class TrackingResponse {

    @Schema(description = "Service request ID", example = "req-123")
    private String requestId;
    
    @Schema(description = "Current tracking status", example = "EN_ROUTE")
    private TrackingStatus trackingStatus;

    // ── Request info ─────────────────────────────────────
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
    
    @Schema(description = "Address of the breakdown location", example = "MG Road, Bangalore")
    private String address;

    // ── ETA & distance ───────────────────────────────────
    @Schema(description = "Estimated time of arrival (formatted)", example = "15 mins")
    private String eta;
    
    @Schema(description = "Distance remaining (formatted)", example = "5.2 km")
    private String distanceRemaining;
    
    @Schema(description = "ETA in seconds", example = "900")
    private long etaSeconds;
    
    @Schema(description = "Distance remaining in meters", example = "5200.0")
    private double distanceMeters;

    // ── User location ────────────────────────────────────
    @Schema(description = "User's latitude", example = "12.9716")
    private double userLat;
    
    @Schema(description = "User's longitude", example = "77.5946")
    private double userLng;

    // ── Mechanic current (simulated) location ────────────
    @Schema(description = "Mechanic's current latitude", example = "12.9345")
    private double mechanicLat;
    
    @Schema(description = "Mechanic's current longitude", example = "77.6123")
    private double mechanicLng;

    // ── Mechanic details ─────────────────────────────────
    @Schema(description = "Mechanic's name", example = "Mike The Mechanic")
    private String mechanicName;
    
    @Schema(description = "Mechanic's phone number", example = "9876543210")
    private String mechanicPhone;
    
    @Schema(description = "Mechanic's vehicle details", example = "Tow Truck KA 02 CD 5678")
    private String mechanicVehicle;
    
    @Schema(description = "Mechanic's average rating", example = "4.8")
    private double mechanicRating;
}
