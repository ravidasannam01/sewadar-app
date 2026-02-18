package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Program-level notification preferences that override global settings.
 * Each program can have its own notification settings per node.
 */
@Entity
@Table(name = "program_notification_preferences", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"program_id", "node_number"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramNotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(name = "node_number", nullable = false)
    private Integer nodeNumber; // 1-6

    @Column(name = "enabled")
    private Boolean enabled; // null = use global, true/false = override

    @Column(name = "message", columnDefinition = "TEXT")
    private String message; // null = use global message, non-null = override with custom message

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

