package com.rssb.application.repository;

import com.rssb.application.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByInchargeIdAndResolvedFalse(Long inchargeId);
    List<Notification> findByInchargeId(Long inchargeId);
    List<Notification> findByProgramIdAndResolvedFalse(Long programId);
}

