package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * ProgramDate entity - represents a date for a program.
 * A program can have multiple dates but one location.
 */
@Entity
@Table(name = "program_dates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramDate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @Column(name = "program_date", nullable = false)
    private LocalDate programDate;

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "SCHEDULED"; // SCHEDULED, COMPLETED, CANCELLED
}

