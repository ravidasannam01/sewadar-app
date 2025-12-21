package com.rssb.application.service;

import com.rssb.application.dto.NotificationResponse;
import com.rssb.application.dto.SewadarResponse;
import com.rssb.application.entity.Notification;
import com.rssb.application.exception.ResourceNotFoundException;
import com.rssb.application.repository.NotificationRepository;
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
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForIncharge(Long inchargeId) {
        log.info("Fetching all notifications for incharge: {}", inchargeId);
        return notificationRepository.findByInchargeId(inchargeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnresolvedNotificationsForIncharge(Long inchargeId) {
        log.info("Fetching unresolved notifications for incharge: {}", inchargeId);
        return notificationRepository.findByInchargeIdAndResolvedFalse(inchargeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public NotificationResponse markAsResolved(Long notificationId, Long inchargeId) {
        log.info("Marking notification {} as resolved by incharge {}", notificationId, inchargeId);
        
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        
        // Verify incharge owns this notification
        if (!notification.getIncharge().getId().equals(inchargeId)) {
            throw new IllegalArgumentException("Only the notification owner can mark it as resolved");
        }
        
        notification.setResolved(true);
        notification.setResolvedAt(LocalDateTime.now());
        notification.setResolvedBy(inchargeId);
        
        Notification saved = notificationRepository.save(notification);
        log.info("Notification {} marked as resolved", notificationId);
        
        return mapToResponse(saved);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        SewadarResponse droppedSewadar = SewadarResponse.builder()
                .id(notification.getDroppedSewadar().getId())
                .firstName(notification.getDroppedSewadar().getFirstName())
                .lastName(notification.getDroppedSewadar().getLastName())
                .mobile(notification.getDroppedSewadar().getMobile())
                .build();

        return NotificationResponse.builder()
                .id(notification.getId())
                .programId(notification.getProgram().getId())
                .programTitle(notification.getProgram().getTitle())
                .droppedSewadar(droppedSewadar)
                .notificationType(notification.getNotificationType())
                .message(notification.getMessage())
                .createdAt(notification.getCreatedAt())
                .resolved(notification.getResolved())
                .resolvedAt(notification.getResolvedAt())
                .build();
    }
}

