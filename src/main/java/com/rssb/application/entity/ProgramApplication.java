package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ProgramApplication entity - when a sewadar applies/consents to a program.
 */
@Entity
@Table(name = "program_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sewadar_id", nullable = false)
    private Sewadar sewadar;

    @Column(name = "applied_at", nullable = false)
    @Builder.Default
    private LocalDateTime appliedAt = LocalDateTime.now();

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, DROP_REQUESTED, DROPPED

    @Column(name = "notes", length = 500)
    private String notes;
    
    @Column(name = "reapply_allowed")
    @Builder.Default
    private Boolean reapplyAllowed = true; // If false, sewadar cannot reapply for this program
    
    @Column(name = "drop_requested_at")
    private LocalDateTime dropRequestedAt; // When sewadar requested to drop
    
    @Column(name = "drop_approved_at")
    private LocalDateTime dropApprovedAt; // When incharge approved drop
    
    @Column(name = "drop_approved_by")
    private Long dropApprovedBy; // Incharge ID who approved drop
}

