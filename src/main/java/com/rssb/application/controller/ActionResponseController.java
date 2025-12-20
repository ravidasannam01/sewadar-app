package com.rssb.application.controller;

import com.rssb.application.dto.ActionResponseRequest;
import com.rssb.application.dto.ActionResponseResponse;
import com.rssb.application.service.ActionResponseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/action-responses")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ActionResponseController {

    private final ActionResponseService responseService;

    @PostMapping
    public ResponseEntity<ActionResponseResponse> submitResponse(@Valid @RequestBody ActionResponseRequest request) {
        log.info("POST /api/action-responses - Submitting response");
        return ResponseEntity.status(HttpStatus.CREATED).body(responseService.submitResponse(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActionResponseResponse> getResponseById(@PathVariable Long id) {
        return ResponseEntity.ok(responseService.getResponseById(id));
    }

    @GetMapping("/action/{actionId}")
    public ResponseEntity<List<ActionResponseResponse>> getResponsesByAction(@PathVariable Long actionId) {
        return ResponseEntity.ok(responseService.getResponsesByAction(actionId));
    }

    @GetMapping("/sewadar/{sewadarId}")
    public ResponseEntity<List<ActionResponseResponse>> getResponsesBySewadar(@PathVariable Long sewadarId) {
        return ResponseEntity.ok(responseService.getResponsesBySewadar(sewadarId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActionResponseResponse> updateResponse(
            @PathVariable Long id,
            @Valid @RequestBody ActionResponseRequest request) {
        return ResponseEntity.ok(responseService.updateResponse(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResponse(@PathVariable Long id) {
        responseService.deleteResponse(id);
        return ResponseEntity.noContent().build();
    }
}

