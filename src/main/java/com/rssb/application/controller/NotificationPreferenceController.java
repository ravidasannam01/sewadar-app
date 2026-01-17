package com.rssb.application.controller;

import com.rssb.application.dto.NotificationPreferenceResponse;
import com.rssb.application.service.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification-preferences")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<List<NotificationPreferenceResponse>> getAllPreferences() {
        return ResponseEntity.ok(preferenceService.getAllPreferences());
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<NotificationPreferenceResponse> togglePreference(
            @PathVariable Long id,
            @RequestParam Boolean enabled) {
        return ResponseEntity.ok(preferenceService.updatePreference(id, enabled));
    }
}

