package com.serviceonwheels.auth_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request object for creating a new service request")
public class CreateServiceRequest {

    @Schema(description = "Type of the vehicle", example = "Car")
    @NotBlank(message = "Vehicle type is required")
    @Size(max = 80, message = "Vehicle type must be at most 80 characters")
    private String vehicleType;

    @Schema(description = "Vehicle registration number", example = "AB 12 CD 3456")
    @NotBlank(message = "Vehicle number is required")
    @Size(max = 32, message = "Vehicle number must be at most 32 characters")
    private String vehicleNumber;

    @Schema(description = "Detailed description of the problem", example = "Engine is overheating after driving for 10 minutes")
    @NotBlank(message = "Problem description is required")
    @Size(max = 4000, message = "Problem description must be at most 4000 characters")
    private String problemDescription;

    @Schema(description = "Pre-selected issue category", example = "Engine Heating")
    @Size(max = 255)
    private String selectedIssue;

    @Schema(description = "Any additional notes", example = "Please bring extra coolant")
    @Size(max = 4000)
    private String additionalNotes;

    @Schema(description = "Latitude of the breakdown location", example = "12.9716")
    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @Schema(description = "Longitude of the breakdown location", example = "77.5946")
    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @Schema(description = "Formatted address of the location", example = "MG Road, Bangalore")
    @NotBlank(message = "Address is required")
    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;
}
