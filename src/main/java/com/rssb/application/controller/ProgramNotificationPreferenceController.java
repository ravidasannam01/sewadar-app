package com.rssb.application.controller;

import com.rssb.application.dto.ProgramNotificationPreferenceResponse;
import com.rssb.application.service.ProgramNotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/program-notification-preferences")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProgramNotificationPreferenceController {

    private final ProgramNotificationPreferenceService preferenceService;

    @GetMapping("/program/{programId}")
    public ResponseEntity<List<ProgramNotificationPreferenceResponse>> getPreferencesForProgram(
            @PathVariable Long programId) {
        return ResponseEntity.ok(preferenceService.getPreferencesForProgram(programId));
    }

    @PutMapping("/program/{programId}/node/{nodeNumber}")
    public ResponseEntity<ProgramNotificationPreferenceResponse> updatePreference(
            @PathVariable Long programId,
            @PathVariable Integer nodeNumber,
            @RequestParam(required = false) Boolean enabled) {
        return ResponseEntity.ok(preferenceService.updatePreference(programId, nodeNumber, enabled));
    }
}

