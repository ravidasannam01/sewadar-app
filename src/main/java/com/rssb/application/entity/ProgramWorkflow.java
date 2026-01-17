package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks the workflow progress for each program.
 * Each program has one workflow that moves through sequential nodes.
 */
@Entity
@Table(name = "program_workflows")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramWorkflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false, unique = true)
    private Program program;

    @Column(name = "current_node", nullable = false)
    @Builder.Default
    private Integer currentNode = 1; // 1-6 (workflow steps)

    @Column(name = "form_released", nullable = false)
    @Builder.Default
    private Boolean formReleased = false;

    @Column(name = "details_collected", nullable = false)
    @Builder.Default
    private Boolean detailsCollected = false;

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

