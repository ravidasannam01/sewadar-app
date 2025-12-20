package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Schedule entity representing scheduled events.
 * This entity has a relationship with Sewadar entity (attended_by).
 */
@Entity
@Table(name = "schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheduled_place", nullable = false, length = 255)
    private String scheduledPlace;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    @Column(name = "scheduled_medium", length = 100)
    private String scheduledMedium;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attended_by", nullable = false)
    private Sewadar attendedBy;
}

