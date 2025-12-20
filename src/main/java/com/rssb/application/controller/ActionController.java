package com.rssb.application.controller;

import com.rssb.application.dto.ActionRequest;
import com.rssb.application.dto.ActionResponseDTO;
import com.rssb.application.service.ActionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/actions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ActionController {

    private final ActionService actionService;

    @PostMapping
    public ResponseEntity<ActionResponseDTO> createAction(@Valid @RequestBody ActionRequest request) {
        log.info("POST /api/actions - Creating action");
        return ResponseEntity.status(HttpStatus.CREATED).body(actionService.createAction(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActionResponseDTO> getActionById(@PathVariable Long id) {
        return ResponseEntity.ok(actionService.getActionById(id));
    }

    @GetMapping("/program/{programId}")
    public ResponseEntity<List<ActionResponseDTO>> getActionsByProgram(@PathVariable Long programId) {
        return ResponseEntity.ok(actionService.getActionsByProgram(programId));
    }

    @GetMapping("/program/{programId}/sewadar/{sewadarId}")
    public ResponseEntity<List<ActionResponseDTO>> getActionsForSewadar(
            @PathVariable Long programId,
            @PathVariable Long sewadarId) {
        return ResponseEntity.ok(actionService.getActionsForSewadar(sewadarId, programId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActionResponseDTO> updateAction(
            @PathVariable Long id,
            @Valid @RequestBody ActionRequest request) {
        return ResponseEntity.ok(actionService.updateAction(id, request));
    }

    @PutMapping("/{id}/order")
    public ResponseEntity<ActionResponseDTO> updateOrder(
            @PathVariable Long id,
            @RequestParam Integer newOrder) {
        return ResponseEntity.ok(actionService.updateActionOrder(id, newOrder));
    }

    @PutMapping("/program/{programId}/reorder")
    public ResponseEntity<Void> reorderActions(
            @PathVariable Long programId,
            @RequestBody List<Long> actionIdsInOrder) {
        actionService.reorderActions(programId, actionIdsInOrder);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAction(@PathVariable Long id) {
        actionService.deleteAction(id);
        return ResponseEntity.noContent().build();
    }
}

