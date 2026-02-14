package com.rssb.application.controller;

import com.rssb.application.dto.SewadarFormSubmissionRequest;
import com.rssb.application.dto.SewadarFormSubmissionResponse;
import com.rssb.application.service.SewadarFormSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/form-submissions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SewadarFormSubmissionController {

    private final SewadarFormSubmissionService formSubmissionService;

    @PostMapping
    public ResponseEntity<SewadarFormSubmissionResponse> submitForm(
            @Valid @RequestBody SewadarFormSubmissionRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String sewadarId = (String) auth.getPrincipal();
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(formSubmissionService.submitForm(request, sewadarId));
    }

    @GetMapping("/program/{programId}")
    public ResponseEntity<List<SewadarFormSubmissionResponse>> getSubmissionsForProgram(
            @PathVariable Long programId) {
        return ResponseEntity.ok(formSubmissionService.getSubmissionsForProgram(programId));
    }

    @GetMapping("/program/{programId}/sewadar/{sewadarId}")
    public ResponseEntity<SewadarFormSubmissionResponse> getSubmissionForSewadar(
            @PathVariable Long programId,
            @PathVariable String sewadarId) {
        SewadarFormSubmissionResponse response = formSubmissionService
            .getSubmissionForSewadar(programId, sewadarId);
        
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-submissions")
    public ResponseEntity<List<SewadarFormSubmissionResponse>> getMySubmissions() {
        // This will be handled by getting all programs where sewadar has submissions
        // For now, return empty list - can be implemented later if needed
        return ResponseEntity.ok(List.of());
    }
}

