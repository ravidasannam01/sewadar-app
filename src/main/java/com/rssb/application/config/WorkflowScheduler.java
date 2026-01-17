package com.rssb.application.config;

import com.rssb.application.service.ProgramWorkflowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for sending daily workflow notifications.
 * Runs once per day at 9:00 AM.
 */
@Component
@Slf4j
public class WorkflowScheduler {

    private final ProgramWorkflowService workflowService;

    public WorkflowScheduler(@Lazy ProgramWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @Scheduled(cron = "0 0 9 * * ?") // Daily at 9:00 AM
    public void sendDailyNotifications() {
        log.info("Running daily workflow notifications scheduler");
        try {
            workflowService.sendDailyNotifications();
            log.info("Daily workflow notifications completed");
        } catch (Exception e) {
            log.error("Error in daily workflow notifications scheduler", e);
        }
    }
}

