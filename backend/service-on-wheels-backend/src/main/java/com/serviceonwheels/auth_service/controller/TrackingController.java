package com.serviceonwheels.auth_service.controller;

import com.serviceonwheels.auth_service.dto.TrackingResponse;
import com.serviceonwheels.auth_service.service.TrackingSseService;
import com.serviceonwheels.auth_service.service.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;

/**
 * REST controller for real-time mechanic tracking.
 * Separated from {@link ServiceRequestController} to keep concerns isolated.
 */
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;
    private final TrackingSseService trackingSseService;

    /**
     * Returns the current tracking state for a service request.
     * This endpoint is kept for compatibility and fallback.
     *
     * @param requestId the service request ID
     * @param principal the authenticated user
     * @return tracking response with mechanic position, ETA, and status
     */
    @GetMapping("/{requestId}")
    public ResponseEntity<TrackingResponse> getTracking(
            @PathVariable("requestId") String requestId,
            Principal principal) {
        TrackingResponse response = trackingService.getTracking(requestId, principal.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Opens a Server-Sent Events stream for real-time tracking updates.
     * The browser connects once and receives live mechanic positions.
     *
     * @param requestId the service request ID
     * @param principal the authenticated user
     * @return an SSE emitter that publishes tracking updates
     */
    @GetMapping("/stream/{requestId}")
    public SseEmitter streamTracking(
            @PathVariable("requestId") String requestId,
            Principal principal) {
        return trackingSseService.openTrackingStream(requestId, principal.getName());
    }
}
