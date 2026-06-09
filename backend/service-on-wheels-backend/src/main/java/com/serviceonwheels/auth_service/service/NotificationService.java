package com.serviceonwheels.auth_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    public void notifyMechanicAssigned(String requestId) {
        log.debug("[Notification] Mechanic assigned for request {}", requestId);
    }

    public void notifyMechanicArriving(String requestId) {
        log.debug("[Notification] Mechanic arriving for request {}", requestId);
    }

    public void notifyServiceCompleted(String requestId) {
        log.debug("[Notification] Service completed for request {}", requestId);
    }
}
