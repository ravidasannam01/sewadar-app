package com.rssb.application.service;

import lombok.extern.slf4j.Slf4j;
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
 * Service for sending WhatsApp messages using WhatsApp Cloud API.
 * 
 * Setup required:
 * 1. Create Meta App at https://developers.facebook.com/
 * 2. Add WhatsApp product to your app
 * 3. Get Access Token and Phone Number ID
 * 4. Configure in application.properties
 */
@Service
@Slf4j
public class WhatsAppService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${whatsapp.api.url:https://graph.facebook.com/v18.0}")
    private String whatsappApiUrl;

    @Value("${whatsapp.phone.number.id:}")
    private String phoneNumberId;

    @Value("${whatsapp.access.token:}")
    private String accessToken;

    @Value("${whatsapp.enabled:false}")
    private boolean whatsappEnabled;

    /**
     * Send WhatsApp message to a phone number.
     * 
     * @param toPhoneNumber Phone number in format: 91XXXXXXXXXX (country code + number)
     * @param message Message text to send
     * @return true if sent successfully, false otherwise
     */
    public boolean sendMessage(String toPhoneNumber, String message) {
        if (!whatsappEnabled || phoneNumberId.isEmpty() || accessToken.isEmpty()) {
            log.warn("WhatsApp is not enabled or not configured. Message not sent: {}", message);
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
                log.info("WhatsApp message sent successfully to {}", formattedPhone);
                return true;
            } else {
                log.error("Failed to send WhatsApp message. Status: {}, Response: {}", 
                    response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending WhatsApp message to {}: {}", toPhoneNumber, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if WhatsApp is properly configured.
     */
    public boolean isConfigured() {
        return whatsappEnabled && !phoneNumberId.isEmpty() && !accessToken.isEmpty();
    }
}
