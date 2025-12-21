package com.rssb.application.controller;

import com.rssb.application.dto.NotificationResponse;
import com.rssb.application.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/incharge/{inchargeId}")
    public ResponseEntity<List<NotificationResponse>> getNotificationsForIncharge(
            @PathVariable Long inchargeId) {
        log.info("GET /api/notifications/incharge/{}", inchargeId);
        return ResponseEntity.ok(notificationService.getNotificationsForIncharge(inchargeId));
    }

    @GetMapping("/incharge/{inchargeId}/unresolved")
    public ResponseEntity<List<NotificationResponse>> getUnresolvedNotificationsForIncharge(
            @PathVariable Long inchargeId) {
        log.info("GET /api/notifications/incharge/{}/unresolved", inchargeId);
        return ResponseEntity.ok(notificationService.getUnresolvedNotificationsForIncharge(inchargeId));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<NotificationResponse> markAsResolved(
            @PathVariable Long id,
            @RequestParam Long inchargeId) {
        log.info("PUT /api/notifications/{}/resolve - incharge: {}", id, inchargeId);
        return ResponseEntity.ok(notificationService.markAsResolved(id, inchargeId));
    }
}

