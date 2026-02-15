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
 * Allows creating the first admin or incharge when no admin/incharge exists in the system.
 * Both admin and incharge can coexist.
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
     * Check if system needs bootstrap (no admin or incharge exists)
     */
    @GetMapping("/status")
    public ResponseEntity<BootstrapStatus> getBootstrapStatus() {
        boolean hasAdmin = !sewadarRepository.findByRole(Role.ADMIN).isEmpty();
        boolean hasIncharge = !sewadarRepository.findByRole(Role.INCHARGE).isEmpty();
        boolean needsBootstrap = !hasAdmin && !hasIncharge;
        
        String message;
        if (hasAdmin && hasIncharge) {
            message = "System is already bootstrapped with both admin and incharge.";
        } else if (hasAdmin) {
            message = "System has an admin. You can create an incharge if needed.";
        } else if (hasIncharge) {
            message = "System has an incharge. You can create an admin if needed.";
        } else {
            message = "No admin or incharge found. Please create the first admin or incharge.";
        }
        
        return ResponseEntity.ok(BootstrapStatus.builder()
                .needsBootstrap(needsBootstrap)
                .hasAdmin(hasAdmin)
                .hasIncharge(hasIncharge)
                .message(message)
                .build());
    }

    /**
     * Create the first admin (only works if no admin exists)
     */
    @PostMapping("/create-admin")
    public ResponseEntity<SewadarResponse> createFirstAdmin(@Valid @RequestBody SewadarRequest request) {
        log.info("Bootstrap: Creating first admin");

        // Check if admin already exists
        if (!sewadarRepository.findByRole(Role.ADMIN).isEmpty()) {
            throw new IllegalArgumentException("System already has an admin. Use promote endpoint instead.");
        }

        // Ensure password is provided for admin
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required for admin creation");
        }

        // Force role to ADMIN
        request.setRole("ADMIN");

        // Create as admin
        SewadarResponse response = sewadarService.createSewadar(request);
        log.info("First admin created with zonal_id: {}", response.getZonalId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
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
        log.info("First incharge created with zonal_id: {}", response.getZonalId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BootstrapStatus {
        private boolean needsBootstrap;
        private boolean hasAdmin;
        private boolean hasIncharge;
        private String message;
    }
}

