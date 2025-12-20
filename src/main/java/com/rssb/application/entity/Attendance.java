package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Attendance entity - final attendance record after program completion.
 */
@Entity
@Table(name = "attendances")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sewadar_id", nullable = false)
    private Sewadar sewadar;

    @Column(name = "attended", nullable = false)
    @Builder.Default
    private Boolean attended = false;

    @Column(name = "marked_by", nullable = false)
    private Long markedBy; // Incharge ID who marked attendance

    @Column(name = "marked_at", nullable = false)
    @Builder.Default
    private LocalDateTime markedAt = LocalDateTime.now();

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "days_participated")
    private Integer daysParticipated;
}

