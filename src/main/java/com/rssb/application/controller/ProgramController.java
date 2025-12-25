package com.rssb.application.controller;

import com.rssb.application.dto.ProgramRequest;
import com.rssb.application.dto.ProgramResponse;
import com.rssb.application.service.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
@SuppressWarnings("unused") // REST endpoints are used via HTTP, not direct Java calls
public class ProgramController {

    private final ProgramService programService;

    @PostMapping
    public ResponseEntity<ProgramResponse> createProgram(@Valid @RequestBody ProgramRequest request) {
        log.info("POST /api/programs - Creating program");
        ProgramResponse created = programService.createProgram(request);
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
    public ResponseEntity<List<ProgramResponse>> getProgramsByIncharge(@PathVariable Long inchargeId) {
        log.info("GET /api/programs/incharge/{} - Fetching programs for incharge", inchargeId);
        return ResponseEntity.ok(programService.getProgramsByIncharge(inchargeId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProgramResponse> updateProgram(
            @PathVariable Long id,
            @Valid @RequestBody ProgramRequest request) {
        log.info("PUT /api/programs/{} - Updating program", id);
        return ResponseEntity.ok(programService.updateProgram(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProgram(@PathVariable Long id) {
        log.info("DELETE /api/programs/{} - Deleting program", id);
        programService.deleteProgram(id);
        return ResponseEntity.noContent().build();
    }
}

