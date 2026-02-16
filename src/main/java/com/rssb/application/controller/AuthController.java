package com.rssb.application.controller;

import com.rssb.application.dto.LoginRequest;
import com.rssb.application.dto.LoginResponse;
import com.rssb.application.service.AuthService;
import com.rssb.application.util.ActionLogger;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final ActionLogger actionLogger;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/auth/login - Login attempt for zonalId: {}", request.getZonalId());
        
        long startTime = System.currentTimeMillis();
        LoginResponse response = authService.login(request);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("zonalId", request.getZonalId());
        details.put("role", response.getSewadar() != null ? response.getSewadar().getRole() : "UNKNOWN");
        details.put("name", response.getSewadar() != null ? 
                response.getSewadar().getFirstName() + " " + response.getSewadar().getLastName() : "UNKNOWN");
        details.put("durationMs", duration);
        actionLogger.logAction("LOGIN", request.getZonalId(), response.getSewadar() != null ? response.getSewadar().getRole() : "UNKNOWN", details);
        actionLogger.logPerformance("LOGIN", duration);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        log.info("POST /api/auth/logout - Logout");
        String token = authHeader.replace("Bearer ", "");
        
        // Log the logout action
        try {
            actionLogger.logAction("LOGOUT", "SYSTEM", "SYSTEM", 
                    Map.of("action", "User logged out"));
        } catch (Exception e) {
            // Ignore - logging is optional
        }
        
        authService.logout(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(authService.validateToken(token));
    }
}

