package com.rssb.application.service;

import com.rssb.application.dto.ProgramNotificationPreferenceResponse;
import com.rssb.application.entity.Program;
import com.rssb.application.entity.NotificationPreference;
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
    private final NotificationPreferenceRepository globalPreferenceRepository;

    /**
     * Get notification preferences for a program.
     * Returns program-level settings with effective enabled status.
     */
    @Transactional(readOnly = true)
    public List<ProgramNotificationPreferenceResponse> getPreferencesForProgram(Long programId) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

        // Get all global preferences
        List<NotificationPreference> globalPrefs = globalPreferenceRepository.findAllByOrderByNodeNumberAsc();
        
        // Get program-level preferences
        List<ProgramNotificationPreference> programPrefs = programPreferenceRepository.findByProgram(program);

        return globalPrefs.stream().map(global -> {
            // Find program-level override if exists
            ProgramNotificationPreference programPref = programPrefs.stream()
                .filter(p -> p.getNodeNumber().equals(global.getNodeNumber()))
                .findFirst()
                .orElse(null);

            Boolean effectiveEnabled = programPref != null && programPref.getEnabled() != null
                ? programPref.getEnabled()
                : global.getEnabled();

            return ProgramNotificationPreferenceResponse.builder()
                .id(programPref != null ? programPref.getId() : null)
                .programId(programId)
                .nodeNumber(global.getNodeNumber())
                .nodeName(global.getNodeName())
                .enabled(programPref != null ? programPref.getEnabled() : null)
                .effectiveEnabled(effectiveEnabled)
                .build();
        }).collect(Collectors.toList());
    }

    /**
     * Update program-level notification preference.
     * @param programId Program ID
     * @param nodeNumber Node number (1-6)
     * @param enabled null = use global, true/false = override
     */
    public ProgramNotificationPreferenceResponse updatePreference(
            Long programId, Integer nodeNumber, Boolean enabled) {
        Program program = programRepository.findById(programId)
            .orElseThrow(() -> new ResourceNotFoundException("Program", "id", programId));

        ProgramNotificationPreference preference = programPreferenceRepository
            .findByProgramAndNodeNumber(program, nodeNumber)
            .orElse(null);

        if (preference == null) {
            preference = ProgramNotificationPreference.builder()
                .program(program)
                .nodeNumber(nodeNumber)
                .enabled(enabled)
                .build();
        } else {
            preference.setEnabled(enabled);
        }

        preference = programPreferenceRepository.save(preference);

        // Get global preference for response
        NotificationPreference globalPref = globalPreferenceRepository
            .findByNodeNumber(nodeNumber)
            .orElse(null);

        Boolean effectiveEnabled = enabled != null ? enabled : 
            (globalPref != null ? globalPref.getEnabled() : false);

        return ProgramNotificationPreferenceResponse.builder()
            .id(preference.getId())
            .programId(programId)
            .nodeNumber(nodeNumber)
            .nodeName(globalPref != null ? globalPref.getNodeName() : "Node " + nodeNumber)
            .enabled(enabled)
            .effectiveEnabled(effectiveEnabled)
            .build();
    }
}

