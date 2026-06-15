package com.serviceonwheels.auth_service.controller;

import com.serviceonwheels.auth_service.dto.ApiResponse;
import com.serviceonwheels.auth_service.dto.MechanicResponse;
import com.serviceonwheels.auth_service.service.MechanicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for mechanic data.
 * All endpoints require JWT authentication.
 */
@RestController
@RequestMapping("/api/mechanics")
@RequiredArgsConstructor
@Tag(name = "Mechanic Controller", description = "Endpoints for retrieving mechanic details")
public class MechanicController {

    private final MechanicService mechanicService;

    @Operation(summary = "List all mechanics", description = "Retrieves a list of all available mechanics")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MechanicResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success("Mechanics retrieved successfully", mechanicService.listAll()));
    }

    @Operation(summary = "Get mechanic by ID", description = "Retrieves detailed information of a specific mechanic")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MechanicResponse>> getById(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success("Mechanic retrieved successfully", mechanicService.findById(id)));
    }
}
