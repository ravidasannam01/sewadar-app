package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Attendance entity - tracks attendance for sewadars in programs by date.
 * Normalized approach: One row per sewadar-program-date combination.
 * References program_dates table for referential integrity.
 */
@Entity
@Table(name = "attendances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"program_date_id", "sewadar_id"})
})
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
    private Program program; // Denormalized for easier queries (can also get from programDate.program)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sewadar_id", referencedColumnName = "zonal_id", nullable = false)
    private Sewadar sewadar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_date_id", nullable = false)
    private ProgramDate programDate; // Foreign key to program_dates - ensures referential integrity

    @Column(name = "marked_at", nullable = false)
    @Builder.Default
    private LocalDateTime markedAt = LocalDateTime.now(); // When this attendance was marked

    @Column(name = "notes", length = 500)
    private String notes; // Optional notes for this attendance record
}

