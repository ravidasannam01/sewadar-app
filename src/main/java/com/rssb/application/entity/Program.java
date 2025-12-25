package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Program entity representing a spiritual program/event.
 */
@Entity
@Table(name = "programs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "location", nullable = false, length = 255)
    private String location; // If location is 'BEAS', it's BEAS location, otherwise NON_BEAS

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "UPCOMING"; // UPCOMING, IN_PROGRESS, COMPLETED, CANCELLED

    @Column(name = "max_sewadars")
    private Integer maxSewadars;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "zonal_id", nullable = false)
    private Sewadar createdBy; // Incharge who created the program

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProgramApplication> applications = new ArrayList<>();

    @OneToMany(mappedBy = "program", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProgramDate> programDates = new ArrayList<>();
}

