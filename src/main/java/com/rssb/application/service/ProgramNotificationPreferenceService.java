package com.rssb.application.service;

import com.rssb.application.dto.ProgramNotificationPreferenceResponse;
import com.rssb.application.entity.Program;
import com.rssb.application.entity.ProgramNotificationPreference;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.ProgramNotificationPreferenceRepository;
import com.rssb.application.repository.ProgramRepository;
import com.rssb.application.repository.NotificationPreferenceRepository;
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
public class ProgramNotificationPreferenceService {

    private final ProgramNotificationPreferenceRepository programPreferenceRepository;
    private final ProgramRepository programRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    // Local copy of node names to keep logic close to workflow definition
    private static final String[] NODE_NAMES = {
            "Make Program Active",
            "Post Application Message",
            "Release Form",
            "Collect Details",
            "Post Mail to Area Secretary",
            "Post General Instructions"
    };

    /**
     * Get notification preferences for a program.
     * Returns program-level settings with global defaults.
     */
    @Transactional(readOnly = true)
    public List<ProgramNotificationPreferenceResponse> getPreferencesForProgram(Long programId) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

        // Get program-level preferences
        List<ProgramNotificationPreference> programPrefs = programPreferenceRepository.findByProgram(program);

        // Ensure we always return 6 nodes in order
        return java.util.stream.IntStream.rangeClosed(1, 6)
                .mapToObj(nodeNumber -> {
                    ProgramNotificationPreference programPref = programPrefs.stream()
                            .filter(p -> p.getNodeNumber().equals(nodeNumber))
                            .findFirst()
                            .orElse(null);

                    boolean enabled = programPref != null && Boolean.TRUE.equals(programPref.getEnabled());
                    
                    // Get global/default message for this node
                    String defaultMessage = notificationPreferenceRepository
                            .findByNodeNumber(nodeNumber)
                            .map(np -> np.getNotificationMessage())
                            .orElse(null);
                    
                    // Get program-level message (null means use default)
                    String programMessage = programPref != null ? programPref.getMessage() : null;
                    boolean isCustomMessage = programMessage != null && !programMessage.trim().isEmpty();

                    return ProgramNotificationPreferenceResponse.builder()
                            .id(programPref != null ? programPref.getId() : null)
                            .programId(programId)
                            .nodeNumber(nodeNumber)
                            .nodeName(nodeNumber <= NODE_NAMES.length ? NODE_NAMES[nodeNumber - 1] : ("Node " + nodeNumber))
                            .enabled(enabled)
                            .effectiveEnabled(enabled)
                            .message(programMessage) // Program-specific message (null = use default)
                            .defaultMessage(defaultMessage) // Global/default message
                            .isCustomMessage(isCustomMessage) // true if using custom message
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Update program-level notification preference.
     * @param programId Program ID
     * @param nodeNumber Node number (1-6)
     * @param enabled true/false to turn notifications on/off for this node (optional)
     * @param message Custom message for this node (optional, null to reset to default)
     */
    public ProgramNotificationPreferenceResponse updatePreference(
            Long programId, Integer nodeNumber, Boolean enabled, String message) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

        ProgramNotificationPreference preference = programPreferenceRepository
            .findByProgramAndNodeNumber(program, nodeNumber)
            .orElse(null);

        if (preference == null) {
            preference = ProgramNotificationPreference.builder()
                .program(program)
                .nodeNumber(nodeNumber)
                .enabled(enabled != null ? Boolean.TRUE.equals(enabled) : null)
                .message(message != null && !message.trim().isEmpty() ? message.trim() : null)
                .build();
        } else {
            if (enabled != null) {
                preference.setEnabled(Boolean.TRUE.equals(enabled));
            }
            // Update message: if null or empty string, set to null (use default)
            // If non-empty, set custom message
            if (message != null) {
                preference.setMessage(message.trim().isEmpty() ? null : message.trim());
            }
        }

        preference = programPreferenceRepository.save(preference);

        // Get default message for response
        String defaultMessage = notificationPreferenceRepository
                .findByNodeNumber(nodeNumber)
                .map(np -> np.getNotificationMessage())
                .orElse(null);
        
        String programMessage = preference.getMessage();
        boolean isCustomMessage = programMessage != null && !programMessage.trim().isEmpty();

        return ProgramNotificationPreferenceResponse.builder()
            .id(preference.getId())
            .programId(programId)
            .nodeNumber(nodeNumber)
            .nodeName(nodeNumber <= NODE_NAMES.length ? NODE_NAMES[nodeNumber - 1] : ("Node " + nodeNumber))
            .enabled(preference.getEnabled())
            .effectiveEnabled(preference.getEnabled())
            .message(programMessage)
            .defaultMessage(defaultMessage)
            .isCustomMessage(isCustomMessage)
            .build();
    }
    
    /**
     * Reset message to default (global message) for a specific node.
     * @param programId Program ID
     * @param nodeNumber Node number (1-6)
     */
    public ProgramNotificationPreferenceResponse resetMessageToDefault(Long programId, Integer nodeNumber) {
        return updatePreference(programId, nodeNumber, null, null);
    }
}

