package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SewadarLanguage entity - represents languages known by a sewadar.
 * Many-to-many relationship between Sewadar and Language.
 */
@Entity
@Table(name = "sewadar_languages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SewadarLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sewadar_id", referencedColumnName = "zonal_id", nullable = false)
    private Sewadar sewadar;

    @Column(name = "language", nullable = false, length = 100)
    private String language; // e.g., "Hindi", "English", "Punjabi", etc.

    @Column(name = "proficiency", length = 50)
    private String proficiency; // e.g., "Basic", "Intermediate", "Fluent", "Native"
}

