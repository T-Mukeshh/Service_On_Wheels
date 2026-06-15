package com.serviceonwheels.auth_service.dto;

import com.serviceonwheels.auth_service.model.MechanicStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response object containing mechanic details")
public class MechanicResponse {

    @Schema(description = "Mechanic's unique identifier", example = "m12345")
    private String id;

    @Schema(description = "Mechanic's name", example = "Mike The Mechanic")
    private String name;

    @Schema(description = "Mechanic's phone number", example = "9876543210")
    private String phone;

    @Schema(description = "Mechanic's average rating", example = "4.8")
    private Double rating;

    @Schema(description = "Mechanic's vehicle type", example = "Tow Truck")
    private String vehicle;

    @Schema(description = "Mechanic's current status", example = "AVAILABLE")
    private MechanicStatus status;

    @Schema(description = "Current latitude", example = "12.9716")
    private Double currentLat;

    @Schema(description = "Current longitude", example = "77.5946")
    private Double currentLng;

    @Schema(description = "ID of the active request if the mechanic is busy", example = "req-12345")
    private String activeRequestId;
}
