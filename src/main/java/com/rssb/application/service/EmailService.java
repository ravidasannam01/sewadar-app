package com.rssb.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Properties;

/**
 * Service for sending emails using SMTP (Gmail or other providers).
 * 
 * Configuration in application.properties:
 * - email.enabled: Enable/disable email sending
 * - email.smtp.host: SMTP server host (e.g., smtp.gmail.com)
 * - email.smtp.port: SMTP port (587 for TLS, 465 for SSL)
 * - email.username: Email address (e.g., your-email@gmail.com)
 * - email.password: App Password or account password
 * - email.from: From email address (defaults to username)
 */
@Service
@Slf4j
public class EmailService {

    private JavaMailSender mailSender;
    private String fromEmail;

    @Value("${email.enabled:false}")
    private boolean emailEnabled;

    @Value("${email.smtp.host:smtp.gmail.com}")
    private String smtpHost;

    @Value("${email.smtp.port:587}")
    private int smtpPort;

    @Value("${email.username:}")
    private String emailUsername;

    @Value("${email.password:}")
    private String emailPassword;

    @Value("${email.from:}")
    private String fromEmailAddress;

    @PostConstruct
    public void init() {
        if (!emailEnabled || emailUsername.isEmpty() || emailPassword.isEmpty()) {
            log.info("Email service is disabled or not configured");
            return;
        }

        try {
            JavaMailSenderImpl mailSenderImpl = new JavaMailSenderImpl();
            mailSenderImpl.setHost(smtpHost);
            mailSenderImpl.setPort(smtpPort);
            mailSenderImpl.setUsername(emailUsername);
            mailSenderImpl.setPassword(emailPassword);

            Properties props = mailSenderImpl.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.debug", "false");

            // For SSL (port 465)
            if (smtpPort == 465) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.ssl.trust", smtpHost);
            }

            mailSender = mailSenderImpl;
            fromEmail = fromEmailAddress.isEmpty() ? emailUsername : fromEmailAddress;
            
            log.info("Email service initialized successfully. SMTP: {}:{}", smtpHost, smtpPort);
        } catch (Exception e) {
            log.error("Failed to initialize email service: {}", e.getMessage(), e);
            mailSender = null;
        }
    }

    /**
     * Send email to a recipient.
     * 
     * @param toEmail Recipient email address
     * @param subject Email subject
     * @param message Email body text
     * @return true if sent successfully, false otherwise
     */
    public boolean sendEmail(String toEmail, String subject, String message) {
        if (!emailEnabled || mailSender == null) {
            log.warn("Email is not enabled or not configured. Email not sent to: {}", toEmail);
            return false;
        }

        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Recipient email is empty. Email not sent.");
            return false;
        }

        try {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(fromEmail);
            email.setTo(toEmail);
            email.setSubject(subject);
            email.setText(message);

            mailSender.send(email);
            log.info("[EMAIL SENT] Recipient: {} | Subject: {} | From: {}", toEmail, subject, fromEmail);
            return true;
        } catch (Exception e) {
            log.error("[EMAIL FAILED] Recipient: {} | Subject: {} | Error: {}", toEmail, subject, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if email service is properly configured.
     */
    public boolean isConfigured() {
        return emailEnabled && mailSender != null && !emailUsername.isEmpty() && !emailPassword.isEmpty();
    }
}

