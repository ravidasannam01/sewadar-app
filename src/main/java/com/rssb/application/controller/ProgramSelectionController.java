package com.rssb.application.controller;

import com.rssb.application.dto.ProgramSelectionRequest;
import com.rssb.application.dto.ProgramSelectionResponse;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.service.ProgramSelectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/program-selections")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ProgramSelectionController {

    private final ProgramSelectionService selectionService;

    @PostMapping
    public ResponseEntity<List<ProgramSelectionResponse>> selectSewadars(@Valid @RequestBody ProgramSelectionRequest request) {
        log.info("POST /api/program-selections - Selecting sewadars");
        return ResponseEntity.status(HttpStatus.CREATED).body(selectionService.selectSewadars(request));
    }

    @GetMapping("/program/{programId}")
    public ResponseEntity<List<ProgramSelectionResponse>> getSelectionsByProgram(@PathVariable Long programId) {
        return ResponseEntity.ok(selectionService.getSelectionsByProgram(programId));
    }

    @GetMapping("/sewadar/{sewadarId}")
    public ResponseEntity<List<ProgramSelectionResponse>> getSelectionsBySewadar(@PathVariable Long sewadarId) {
        return ResponseEntity.ok(selectionService.getSelectionsBySewadar(sewadarId));
    }

    @GetMapping("/program/{programId}/prioritized")
    public ResponseEntity<List<SewadarResponse>> getPrioritizedSewadars(
            @PathVariable Long programId,
            @RequestParam(required = false, defaultValue = "attendance") String sortBy) {
        return ResponseEntity.ok(selectionService.getPrioritizedSewadars(programId, sortBy));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ProgramSelectionResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(selectionService.updateSelectionStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeSelection(@PathVariable Long id) {
        selectionService.removeSelection(id);
        return ResponseEntity.noContent().build();
    }
}

