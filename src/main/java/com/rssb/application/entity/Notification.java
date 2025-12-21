package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Notification entity - for incharge notifications about drop requests that need refilling
 */
@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sewadar_id", nullable = false)
    private Sewadar droppedSewadar; // Sewadar who dropped

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incharge_id", nullable = false)
    private Sewadar incharge; // Incharge who needs to take action

    @Column(name = "notification_type", length = 50)
    @Builder.Default
    private String notificationType = "DROP_REQUEST"; // DROP_REQUEST, REFILL_REQUIRED

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by")
    private Long resolvedBy; // Incharge ID who resolved
}

