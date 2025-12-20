package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ProgramSelection entity - when an incharge selects a sewadar for a program.
 */
@Entity
@Table(name = "program_selections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sewadar_id", nullable = false)
    private Sewadar sewadar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selected_by", nullable = false)
    private Sewadar selectedBy; // Incharge who selected

    @Column(name = "selected_at", nullable = false)
    @Builder.Default
    private LocalDateTime selectedAt = LocalDateTime.now();

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "SELECTED"; // SELECTED, CONFIRMED, DROPPED, REPLACED

    @Column(name = "priority_score")
    private Integer priorityScore; // For sorting/prioritization

    @Column(name = "selection_reason", length = 500)
    private String selectionReason;
}

