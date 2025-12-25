package com.rssb.application.service;

import java.util.List;

/**
 * WhatsApp service interface - placeholder for future implementation.
 * This service will handle sending WhatsApp messages to sewadars.
 */
public interface WhatsAppService {
    /**
     * Send message to a single sewadar
     */
    void sendMessage(String mobileNumber, String message);

    /**
     * Send message to multiple sewadars
     */
    void sendBulkMessage(List<String> mobileNumbers, String message);
}

