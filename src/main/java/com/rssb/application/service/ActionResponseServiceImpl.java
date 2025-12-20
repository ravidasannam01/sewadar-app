package com.rssb.application.service;

import com.rssb.application.dto.ActionResponseRequest;
import com.rssb.application.dto.ActionResponseResponse;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.entity.Action;
import com.rssb.application.entity.ActionResponse;
import com.rssb.application.entity.Sewadar;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.ActionRepository;
import com.rssb.application.repository.ActionResponseRepository;
import com.rssb.application.repository.SewadarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ActionResponseServiceImpl implements ActionResponseService {

    private final ActionResponseRepository responseRepository;
    private final ActionRepository actionRepository;
    private final SewadarRepository sewadarRepository;

    @Override
    public ActionResponseResponse submitResponse(ActionResponseRequest request) {
        log.info("Sewadar {} submitting response to action {}", request.getSewadarId(), request.getActionId());

        Action action = actionRepository.findById(request.getActionId())
                .orElseThrow(() -> new ResourceNotFoundException("Action", "id", request.getActionId()));

        Sewadar sewadar = sewadarRepository.findById(request.getSewadarId())
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "id", request.getSewadarId()));

        // Check if response already exists
        ActionResponse existing = responseRepository.findByActionIdAndSewadarId(request.getActionId(), request.getSewadarId())
                .orElse(null);

        ActionResponse response;
        if (existing != null) {
            // Update existing response
            existing.setResponseData(request.getResponseData());
            existing.setNotes(request.getNotes());
            existing.setStatus("COMPLETED");
            existing.setSubmittedAt(LocalDateTime.now());
            response = responseRepository.save(existing);
            log.info("Updated existing response for action {}", request.getActionId());
        } else {
            // Create new response
            response = ActionResponse.builder()
                    .action(action)
                    .sewadar(sewadar)
                    .responseData(request.getResponseData())
                    .notes(request.getNotes())
                    .status("COMPLETED")
                    .submittedAt(LocalDateTime.now())
                    .build();
            response = responseRepository.save(response);
            log.info("Created new response for action {}", request.getActionId());
        }

        return mapToResponse(response);
    }

    @Override
    @Transactional(readOnly = true)
    public ActionResponseResponse getResponseById(Long id) {
        ActionResponse response = responseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActionResponse", "id", id));
        return mapToResponse(response);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActionResponseResponse> getResponsesByAction(Long actionId) {
        log.info("Fetching responses for action: {}", actionId);
        return responseRepository.findByActionId(actionId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActionResponseResponse> getResponsesBySewadar(Long sewadarId) {
        log.info("Fetching responses for sewadar: {}", sewadarId);
        return responseRepository.findBySewadarId(sewadarId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ActionResponseResponse updateResponse(Long id, ActionResponseRequest request) {
        log.info("Updating response: {}", id);
        ActionResponse response = responseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActionResponse", "id", id));

        response.setResponseData(request.getResponseData());
        response.setNotes(request.getNotes());
        response.setSubmittedAt(LocalDateTime.now());

        ActionResponse updated = responseRepository.save(response);
        return mapToResponse(updated);
    }

    @Override
    public void deleteResponse(Long id) {
        log.info("Deleting response: {}", id);
        ActionResponse response = responseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ActionResponse", "id", id));
        responseRepository.delete(response);
    }

    private ActionResponseResponse mapToResponse(ActionResponse response) {
        SewadarResponse sewadar = SewadarResponse.builder()
                .id(response.getSewadar().getId())
                .firstName(response.getSewadar().getFirstName())
                .lastName(response.getSewadar().getLastName())
                .mobile(response.getSewadar().getMobile())
                .build();

        return ActionResponseResponse.builder()
                .id(response.getId())
                .actionId(response.getAction().getId())
                .actionTitle(response.getAction().getTitle())
                .sewadar(sewadar)
                .responseData(response.getResponseData())
                .status(response.getStatus())
                .submittedAt(response.getSubmittedAt())
                .notes(response.getNotes())
                .build();
    }
}

