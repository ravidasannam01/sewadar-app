package com.rssb.application.service;

import com.rssb.application.dto.NotificationResponse;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getNotificationsForIncharge(Long inchargeId);
    List<NotificationResponse> getUnresolvedNotificationsForIncharge(Long inchargeId);
    NotificationResponse markAsResolved(Long notificationId, Long inchargeId);
}

