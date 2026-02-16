package com.rssb.application.controller;

import com.rssb.application.dto.ProgramRequest;
import com.rssb.application.dto.ProgramResponse;
import com.rssb.application.service.ProgramService;
import com.rssb.application.util.ActionLogger;
import com.rssb.application.util.UserContextUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("unused") // REST endpoints are used via HTTP, not direct Java calls
public class ProgramController {

    private final ProgramService programService;
    private final ActionLogger actionLogger;

    @PostMapping
    public ResponseEntity<ProgramResponse> createProgram(@Valid @RequestBody ProgramRequest request) {
        log.info("POST /api/programs - Creating program");
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        ProgramResponse created = programService.createProgram(request);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("programId", created.getId());
        details.put("title", created.getTitle());
        details.put("location", created.getLocation());
        details.put("status", created.getStatus());
        details.put("maxSewadars", created.getMaxSewadars());
        details.put("createdBy", request.getCreatedById());
        details.put("durationMs", duration);
        actionLogger.logAction("CREATE_PROGRAM", userId, userRole, details);
        actionLogger.logPerformance("CREATE_PROGRAM", duration);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ProgramResponse>> getAllPrograms() {
        log.info("GET /api/programs - Fetching all programs");
        return ResponseEntity.ok(programService.getAllPrograms());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProgramResponse> getProgramById(@PathVariable Long id) {
        log.info("GET /api/programs/{} - Fetching program", id);
        return ResponseEntity.ok(programService.getProgramById(id));
    }

    /**
     * Get programs created by an incharge
     * @param inchargeId Incharge zonal ID
     * @return List of programs
     */
    @GetMapping("/incharge/{inchargeId}")
    public ResponseEntity<List<ProgramResponse>> getProgramsByIncharge(@PathVariable String inchargeId) {
        log.info("GET /api/programs/incharge/{} - Fetching programs for incharge", inchargeId);
        return ResponseEntity.ok(programService.getProgramsByIncharge(inchargeId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProgramResponse> updateProgram(
            @PathVariable Long id,
            @Valid @RequestBody ProgramRequest request) {
        log.info("PUT /api/programs/{} - Updating program", id);
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        long startTime = System.currentTimeMillis();
        ProgramResponse updated = programService.updateProgram(id, request);
        long duration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> details = new HashMap<>();
        details.put("programId", id);
        details.put("title", updated.getTitle());
        details.put("status", updated.getStatus());
        details.put("durationMs", duration);
        actionLogger.logAction("UPDATE_PROGRAM", userId, userRole, details);
        actionLogger.logPerformance("UPDATE_PROGRAM", duration);
        
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProgram(@PathVariable Long id) {
        log.info("DELETE /api/programs/{} - Deleting program", id);
        String userId = UserContextUtil.getCurrentUserId();
        String userRole = UserContextUtil.getCurrentUserRole();
        
        // Get program details before deletion for logging
        try {
            ProgramResponse program = programService.getProgramById(id);
            Map<String, Object> details = new HashMap<>();
            details.put("programId", id);
            details.put("title", program.getTitle());
            actionLogger.logAction("DELETE_PROGRAM", userId, userRole, details);
        } catch (Exception e) {
            Map<String, Object> details = new HashMap<>();
            details.put("programId", id);
            actionLogger.logAction("DELETE_PROGRAM", userId, userRole, details);
        }
        
        programService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }
}

