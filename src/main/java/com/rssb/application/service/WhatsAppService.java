package com.rssb.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending WhatsApp messages.
 * Supports two modes:
 * 1. "api" - Official WhatsApp Cloud API (requires business verification)
 * 2. "email-bridge" - Sends email with WhatsApp instructions, another machine handles actual sending
 * 
 * Configure via application.properties:
 * - whatsapp.mode=api or email-bridge
 * - whatsapp.enabled=true/false
 */
@Service
@Slf4j
public class WhatsAppService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private EmailService emailService;

    @Value("${whatsapp.mode:api}")
    private String whatsappMode; // "api" or "email-bridge"

    @Value("${whatsapp.api.url:https://graph.facebook.com/v18.0}")
    private String whatsappApiUrl;

    @Value("${whatsapp.phone.number.id:}")
    private String phoneNumberId;

    @Value("${whatsapp.access.token:}")
    private String accessToken;

    @Value("${whatsapp.bridge.email:}")
    private String bridgeEmail;

    @Value("${whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    /**
     * Send WhatsApp message to a phone number.
     * Uses either Cloud API or Email Bridge based on configuration.
     * 
     * @param toPhoneNumber Phone number in format: 91XXXXXXXXXX (country code + number)
     * @param message Message text to send
     * @return true if sent successfully, false otherwise
     */
    public boolean sendMessage(String toPhoneNumber, String message) {
        if (!whatsappEnabled) {
            log.warn("WhatsApp is not enabled. Message not sent: {}", message);
            return false;
        }

        if ("email-bridge".equals(whatsappMode)) {
            return sendViaEmailBridge(toPhoneNumber, message);
        } else {
            return sendViaCloudAPI(toPhoneNumber, message);
        }
    }

    /**
     * Send message via WhatsApp Cloud API (official method).
     */
    private boolean sendViaCloudAPI(String toPhoneNumber, String message) {
        if (phoneNumberId.isEmpty() || accessToken.isEmpty()) {
            log.warn("[CLOUD API] WhatsApp Cloud API is not configured. Phone Number ID or Access Token missing.");
            return false;
        }

        try {
            // Format phone number (remove + and spaces)
            String formattedPhone = toPhoneNumber.replaceAll("[^0-9]", "");
            
            // WhatsApp Cloud API endpoint
            String url = String.format("%s/%s/messages", whatsappApiUrl, phoneNumberId);

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);

            // Request body
            Map<String, Object> body = new HashMap<>();
            body.put("messaging_product", "whatsapp");
            body.put("to", formattedPhone);
            
            Map<String, String> textContent = new HashMap<>();
            textContent.put("body", message);
            body.put("text", textContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // Send request
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("[CLOUD API] WhatsApp message sent successfully to {}", formattedPhone);
                return true;
            } else {
                log.error("[CLOUD API] Failed to send WhatsApp message. Status: {}, Response: {}", 
                    response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("[CLOUD API] Error sending WhatsApp message to {}: {}", toPhoneNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send message via Email Bridge (hacky solution).
     * Sends an email with WhatsApp instructions that another machine will process.
     */
    private boolean sendViaEmailBridge(String toPhoneNumber, String message) {
        // This method is kept for backward compatibility
        // But batch sending is preferred - use sendBatchMessages instead
        java.util.List<String> phoneNumbers = new java.util.ArrayList<>();
        phoneNumbers.add(toPhoneNumber);
        return sendBatchMessages(phoneNumbers, message, null, null);
    }

    /**
     * Send batch WhatsApp messages via Email Bridge.
     * Sends ONE email with all recipients and message(s).
     * 
     * @param phoneNumbers List of phone numbers (with country code, e.g., "916303623749")
     * @param commonMessage Common message for all recipients (if all get same message)
     * @param programTitle Optional program title for context
     * @param nodeNumber Optional node number for context
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendBatchMessages(java.util.List<String> phoneNumbers, String commonMessage, 
                                     String programTitle, Integer nodeNumber) {
        if (bridgeEmail == null || bridgeEmail.trim().isEmpty()) {
            log.warn("[EMAIL BRIDGE] Bridge email not configured. Set whatsapp.bridge.email in application.properties");
            return false;
        }
        
        String bridgeEmailTrimmed = bridgeEmail.trim();

        if (!emailService.isConfigured()) {
            log.warn("[EMAIL BRIDGE] Email service is not configured. Cannot send WhatsApp trigger email.");
            return false;
        }

        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            log.warn("[EMAIL BRIDGE] No phone numbers provided for batch send");
            return false;
        }

        try {
            // Format all phone numbers (remove + and spaces)
            java.util.List<String> formattedPhones = new java.util.ArrayList<>();
            for (String phone : phoneNumbers) {
                if (phone != null && !phone.trim().isEmpty()) {
                    String formatted = phone.replaceAll("[^0-9]", "");
                    if (!formatted.isEmpty()) {
                        formattedPhones.add(formatted);
                    }
                }
            }

            if (formattedPhones.isEmpty()) {
                log.warn("[EMAIL BRIDGE] No valid phone numbers after formatting");
                return false;
            }

            // Create email subject (identifies it as WhatsApp trigger)
            String subject = "WHATSAPP_SEND";
            
            // Create email body with structured format
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("PROGRAM: ").append(programTitle != null ? programTitle : "N/A").append("\n");
            emailBody.append("NODE: ").append(nodeNumber != null ? nodeNumber : "N/A").append("\n");
            emailBody.append("MESSAGE: ").append(commonMessage != null ? commonMessage : "").append("\n");
            emailBody.append("\n");
            emailBody.append("RECIPIENTS:\n");
            for (String phone : formattedPhones) {
                emailBody.append(phone).append("\n");
            }
            emailBody.append("\n");
            emailBody.append("---\n");
            emailBody.append("This is an automated WhatsApp trigger email.\n");
            emailBody.append("Another machine should read this and send WhatsApp messages to all recipients listed above.\n");
            emailBody.append("All recipients receive the same message.\n");
            emailBody.append("Timestamp: ").append(java.time.LocalDateTime.now()).append("\n");
            emailBody.append("Total Recipients: ").append(formattedPhones.size()).append("\n");

            // Send email
            boolean emailSent = emailService.sendEmail(bridgeEmailTrimmed, subject, emailBody.toString());
            
            if (emailSent) {
                log.info("[EMAIL BRIDGE] WhatsApp batch trigger email sent successfully to {} for {} recipients", 
                        bridgeEmailTrimmed, formattedPhones.size());
                return true;
            } else {
                log.error("[EMAIL BRIDGE] Failed to send WhatsApp batch trigger email to {}", bridgeEmailTrimmed);
                return false;
            }
        } catch (Exception e) {
            log.error("[EMAIL BRIDGE] Error sending WhatsApp batch trigger email: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if WhatsApp is properly configured.
     */
    public boolean isConfigured() {
        if (!whatsappEnabled) {
            return false;
        }
        
        if ("email-bridge".equals(whatsappMode)) {
            // For email bridge, check if bridge email and email service are configured
            return !bridgeEmail.trim().isEmpty() && emailService.isConfigured();
        } else {
            // For cloud API, check for required credentials
            return !phoneNumberId.isEmpty() && !accessToken.isEmpty();
        }
    }
}
