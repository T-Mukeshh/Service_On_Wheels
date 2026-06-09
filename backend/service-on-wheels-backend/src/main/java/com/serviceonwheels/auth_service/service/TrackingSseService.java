package com.serviceonwheels.auth_service.service;

import com.serviceonwheels.auth_service.dto.TrackingResponse;
import com.serviceonwheels.auth_service.model.TrackingStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrackingSseService {

    private final TrackingService trackingService;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    });

    public SseEmitter openTrackingStream(String requestId, String userEmail) {
        // Validate access before creating a long-lived stream.
        TrackingResponse initialResponse = trackingService.getTracking(requestId, userEmail);

        SseEmitter emitter = new SseEmitter(0L);
        ScheduledFuture<?> task = executor.scheduleAtFixedRate(
                () -> pushTrackingUpdate(emitter, requestId, userEmail),
                0,
                1,
                TimeUnit.SECONDS
        );

        emitter.onCompletion(() -> task.cancel(true));
        emitter.onTimeout(() -> task.cancel(true));
        emitter.onError((ex) -> task.cancel(true));

        return emitter;
    }

    private void pushTrackingUpdate(SseEmitter emitter, String requestId, String userEmail) {
        try {
            TrackingResponse response = trackingService.getTracking(requestId, userEmail);
            emitter.send(SseEmitter.event().name("tracking-update").data(response));
            if (response.getTrackingStatus() == TrackingStatus.ARRIVED ||
                    response.getTrackingStatus() == TrackingStatus.COMPLETED) {
                emitter.complete();
            }
        } catch (IOException ex) {
            log.warn("Unable to send tracking update for request {}: {}", requestId, ex.getMessage());
            emitter.completeWithError(ex);
        } catch (RuntimeException ex) {
            log.warn("Tracking stream error for request {}: {}", requestId, ex.getMessage());
            emitter.completeWithError(ex);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
