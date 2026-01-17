package com.rssb.application.service;

import com.rssb.application.dto.NotificationPreferenceResponse;
import com.rssb.application.entity.NotificationPreference;
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
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    // Initialize default preferences on first access
    @Transactional
    public void initializeDefaults() {
        if (preferenceRepository.count() == 0) {
            String[] nodeNames = {
                "Make Program Active",
                "Post Application Message",
                "Release Form",
                "Collect Details",
                "Post Mail to Area Secretary",
                "Post General Instructions"
            };

            String[] messages = {
                "Please make the program '{programTitle}' active.",
                "Please post a message in the central WhatsApp group asking sewadars to apply for '{programTitle}' before a specific date.",
                "Applications are full. Please post a message in WhatsApp group with form details for '{programTitle}'.",
                "Please post a mail to the area secretary regarding '{programTitle}' at their area.",
                "Please post general instructions regarding '{programTitle}' in the WhatsApp group."
            };

            for (int i = 0; i < nodeNames.length; i++) {
                NotificationPreference preference = NotificationPreference.builder()
                    .nodeNumber(i + 1)
                    .nodeName(nodeNames[i])
                    .notificationMessage(messages[i])
                    .enabled(true)
                    .build();
                preferenceRepository.save(preference);
            }
            log.info("Initialized default notification preferences");
        }
    }

    public List<NotificationPreferenceResponse> getAllPreferences() {
        // Initialize defaults if needed (this will use write transaction if needed)
        initializeDefaults();
        return preferenceRepository.findAllByOrderByNodeNumberAsc().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }

    public NotificationPreferenceResponse updatePreference(Long id, Boolean enabled) {
        NotificationPreference preference = preferenceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Notification preference not found"));

        preference.setEnabled(enabled);
        preference = preferenceRepository.save(preference);
        log.info("Updated notification preference {} to enabled={}", id, enabled);
        return mapToResponse(preference);
    }

    private NotificationPreferenceResponse mapToResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
            .id(preference.getId())
            .nodeNumber(preference.getNodeNumber())
            .nodeName(preference.getNodeName())
            .notificationMessage(preference.getNotificationMessage())
            .enabled(preference.getEnabled())
            .build();
    }
}

