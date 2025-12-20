package com.rssb.application.controller;

import com.rssb.application.dto.SewadarRequest;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.entity.Role;
import com.rssb.application.repository.SewadarRepository;
import com.rssb.application.service.SewadarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Bootstrap controller for initial setup.
 * Allows creating the first incharge when no incharge exists in the system.
 */
@RestController
@RequestMapping("/api/bootstrap")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BootstrapController {

    private final SewadarRepository sewadarRepository;
    private final SewadarService sewadarService;

    /**
     * Check if system needs bootstrap (no incharge exists)
     */
    @GetMapping("/status")
    public ResponseEntity<BootstrapStatus> getBootstrapStatus() {
        boolean hasIncharge = !sewadarRepository.findByRole(Role.INCHARGE).isEmpty();
        return ResponseEntity.ok(BootstrapStatus.builder()
                .needsBootstrap(!hasIncharge)
                .hasIncharge(hasIncharge)
                .message(hasIncharge 
                        ? "System is already bootstrapped" 
                        : "No incharge found. Please create the first incharge.")
                .build());
    }

    /**
     * Create the first incharge (only works if no incharge exists)
     */
    @PostMapping("/create-incharge")
    public ResponseEntity<SewadarResponse> createFirstIncharge(@Valid @RequestBody SewadarRequest request) {
        log.info("Bootstrap: Creating first incharge");

        // Check if incharge already exists
        if (!sewadarRepository.findByRole(Role.INCHARGE).isEmpty()) {
            throw new IllegalArgumentException("System already has an incharge. Use promote endpoint instead.");
        }

        // Ensure password is provided for incharge
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required for incharge creation");
        }

        // Force role to INCHARGE
        request.setRole("INCHARGE");

        // Create as incharge
        SewadarResponse response = sewadarService.createSewadar(request);
        log.info("First incharge created with id: {}", response.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BootstrapStatus {
        private boolean needsBootstrap;
        private boolean hasIncharge;
        private String message;
    }
}

