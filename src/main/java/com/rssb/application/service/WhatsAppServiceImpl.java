package com.rssb.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * WhatsApp service implementation - placeholder.
 * TODO: Integrate with WhatsApp API (Twilio, WhatsApp Business API, etc.)
 */
@Service
@Slf4j
public class WhatsAppServiceImpl implements WhatsAppService {

    @Override
    public void sendMessage(String mobileNumber, String message) {
        log.info("WhatsApp Service: Sending message to {} - {}", mobileNumber, message);
        // TODO: Implement actual WhatsApp integration
        // Example: Use Twilio WhatsApp API, WhatsApp Business API, etc.
    }

    @Override
    public void sendBulkMessage(List<String> mobileNumbers, String message) {
        log.info("WhatsApp Service: Sending bulk message to {} recipients - {}", mobileNumbers.size(), message);
        mobileNumbers.forEach(number -> sendMessage(number, message));
    }

    @Override
    public void notifyAction(Long programId, Long actionId, String message) {
        log.info("WhatsApp Service: Notifying action {} for program {} - {}", actionId, programId, message);
        // TODO: Get selected sewadars for program and send notifications
    }
}

