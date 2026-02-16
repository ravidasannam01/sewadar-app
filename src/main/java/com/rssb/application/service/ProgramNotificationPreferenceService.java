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
     * Returns program-level settings only (no global preferences).
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

                    return ProgramNotificationPreferenceResponse.builder()
                            .id(programPref != null ? programPref.getId() : null)
                            .programId(programId)
                            .nodeNumber(nodeNumber)
                            .nodeName(nodeNumber <= NODE_NAMES.length ? NODE_NAMES[nodeNumber - 1] : ("Node " + nodeNumber))
                            .enabled(enabled)
                            .effectiveEnabled(enabled)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Update program-level notification preference.
     * @param programId Program ID
     * @param nodeNumber Node number (1-6)
     * @param enabled true/false to turn notifications on/off for this node
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
                .enabled(Boolean.TRUE.equals(enabled))
                .build();
        } else {
            preference.setEnabled(Boolean.TRUE.equals(enabled));
        }

        preference = programPreferenceRepository.save(preference);

        return ProgramNotificationPreferenceResponse.builder()
            .id(preference.getId())
            .programId(programId)
            .nodeNumber(nodeNumber)
            .nodeName(nodeNumber <= NODE_NAMES.length ? NODE_NAMES[nodeNumber - 1] : ("Node " + nodeNumber))
            .enabled(preference.getEnabled())
            .effectiveEnabled(preference.getEnabled())
            .build();
    }
}

