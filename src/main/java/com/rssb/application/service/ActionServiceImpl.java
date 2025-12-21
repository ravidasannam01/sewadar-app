package com.rssb.application.service;

import com.rssb.application.dto.ActionRequest;
import com.rssb.application.dto.ActionResponseDTO;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.entity.Action;
import com.rssb.application.entity.Program;
import com.rssb.application.entity.ProgramSelection;
import com.rssb.application.entity.Role;
import com.rssb.application.entity.Sewadar;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.ActionRepository;
import com.rssb.application.repository.ActionResponseRepository;
import com.rssb.application.repository.ProgramRepository;
import com.rssb.application.repository.ProgramSelectionRepository;
import com.rssb.application.repository.SewadarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ActionServiceImpl implements ActionService {

    private final ActionRepository actionRepository;
    private final ProgramRepository programRepository;
    private final SewadarRepository sewadarRepository;
    private final ProgramSelectionRepository selectionRepository;
    private final ActionResponseRepository actionResponseRepository;
    private final WhatsAppService whatsAppService;

    @Override
    public ActionResponseDTO createAction(ActionRequest request) {
        log.info("Creating action: {} for program {}", request.getTitle(), request.getProgramId());

        Program program = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new ResourceNotFoundException("Program", "id", request.getProgramId()));

        Sewadar incharge = sewadarRepository.findById(request.getCreatedById())
                .orElseThrow(() -> new ResourceNotFoundException("Sewadar", "id", request.getCreatedById()));

        if (incharge.getRole() != Role.INCHARGE) {
            throw new IllegalArgumentException("Only incharge can create actions");
        }

        // Determine sequence order if not provided
        Integer sequenceOrder = request.getSequenceOrder();
        if (sequenceOrder == null) {
            List<Action> existingActions = actionRepository.findByProgramIdOrderBySequenceOrderAsc(request.getProgramId());
            sequenceOrder = existingActions.isEmpty() ? 1 : existingActions.get(existingActions.size() - 1).getSequenceOrder() + 1;
        }

        Action action = Action.builder()
                .program(program)
                .title(request.getTitle())
                .description(request.getDescription())
                .actionType(request.getActionType())
                .createdBy(incharge)
                .dueDate(request.getDueDate())
                .sequenceOrder(sequenceOrder)
                .status("ACTIVE")
                .build();

        Action saved = actionRepository.save(action);
        log.info("Action created with id: {}", saved.getId());

        // Notify selected sewadars via WhatsApp
        // Only notify active selections (exclude DROPPED)
        List<ProgramSelection> selections = selectionRepository.findByProgramId(request.getProgramId()).stream()
                .filter(s -> !"DROPPED".equals(s.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        String message = String.format("New action: %s - %s. Please complete this action.", 
                saved.getTitle(), saved.getDescription());
        
        selections.forEach(selection -> {
            if (selection.getSewadar().getMobile() != null) {
                whatsAppService.sendMessage(selection.getSewadar().getMobile(), message);
            }
        });

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ActionResponseDTO getActionById(Long id) {
        Action action = actionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Action", "id", id));
        return mapToResponse(action);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActionResponseDTO> getActionsByProgram(Long programId) {
        log.info("Fetching actions for program: {}", programId);
        return actionRepository.findByProgramIdOrderBySequenceOrderAsc(programId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActionResponseDTO> getActionsForSewadar(Long sewadarId, Long programId) {
        log.info("Fetching actions for sewadar {} in program {}", sewadarId, programId);
        
        // Check if sewadar is selected for this program
        selectionRepository.findByProgramIdAndSewadarId(programId, sewadarId)
                .orElseThrow(() -> new IllegalArgumentException("Sewadar is not selected for this program"));

        return actionRepository.findByProgramIdOrderBySequenceOrderAsc(programId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ActionResponseDTO updateAction(Long id, ActionRequest request) {
        log.info("Updating action: {}", id);
        Action action = actionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Action", "id", id));

        action.setTitle(request.getTitle());
        action.setDescription(request.getDescription());
        action.setActionType(request.getActionType());
        action.setDueDate(request.getDueDate());
        if (request.getSequenceOrder() != null) {
            action.setSequenceOrder(request.getSequenceOrder());
        }

        Action updated = actionRepository.save(action);
        return mapToResponse(updated);
    }

    @Override
    public ActionResponseDTO updateActionOrder(Long id, Integer newOrder) {
        log.info("Updating action {} order to {}", id, newOrder);
        Action action = actionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Action", "id", id));

        action.setSequenceOrder(newOrder);
        Action updated = actionRepository.save(action);
        return mapToResponse(updated);
    }

    @Override
    public void reorderActions(Long programId, List<Long> actionIdsInOrder) {
        log.info("Reordering actions for program: {}", programId);
        
        for (int i = 0; i < actionIdsInOrder.size(); i++) {
            Long actionId = actionIdsInOrder.get(i);
            Action action = actionRepository.findById(actionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Action", "id", actionId));
            
            if (!action.getProgram().getId().equals(programId)) {
                throw new IllegalArgumentException("Action does not belong to this program");
            }
            
            action.setSequenceOrder(i + 1);
            actionRepository.save(action);
        }
    }

    @Override
    public void deleteAction(Long id) {
        log.info("Deleting action: {}", id);
        Action action = actionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Action", "id", id));
        actionRepository.delete(action);
    }

    private ActionResponseDTO mapToResponse(Action action) {
        SewadarResponse createdBy = SewadarResponse.builder()
                .id(action.getCreatedBy().getId())
                .firstName(action.getCreatedBy().getFirstName())
                .lastName(action.getCreatedBy().getLastName())
                .build();

        // Count responses
        long responseCount = actionResponseRepository.findByActionId(action.getId()).stream()
                .filter(r -> "COMPLETED".equals(r.getStatus()))
                .count();
        
        // Count only active selected sewadars for this program (exclude DROPPED)
        long totalSelected = selectionRepository.findByProgramId(action.getProgram().getId()).stream()
                .filter(s -> !"DROPPED".equals(s.getStatus()))
                .count();
        long pendingCount = totalSelected - responseCount;

        return ActionResponseDTO.builder()
                .id(action.getId())
                .programId(action.getProgram().getId())
                .programTitle(action.getProgram().getTitle())
                .title(action.getTitle())
                .description(action.getDescription())
                .actionType(action.getActionType())
                .createdBy(createdBy)
                .createdAt(action.getCreatedAt())
                .dueDate(action.getDueDate())
                .status(action.getStatus())
                .sequenceOrder(action.getSequenceOrder())
                .responseCount(responseCount)
                .pendingCount(pendingCount)
                .build();
    }
}

