package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Sewadar entity representing a sewadar (volunteer/worker).
 * This entity has a relationship with Address entity.
 */
@Entity
@Table(name = "sewadars")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sewadar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id; // Internal primary key (auto-generated)

    @Column(name = "zonal_id", unique = true, nullable = false, length = 50)
    private String zonalId; // Organizational identity (provided by admin, String type)

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "location", length = 255)
    private String location; // Location/center identified by org (replaces dept)

    @Column(name = "mobile", length = 20, unique = true)
    private String mobile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.SEWADAR;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Column(name = "profession", length = 100)
    private String profession;

    @Column(name = "password", nullable = false)
    private String password; // Encrypted password

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "emergency_contact", length = 20)
    private String emergencyContact;

    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "aadhar_number", length = 12, unique = true)
    private String aadharNumber; // 12-digit Aadhar number

    @OneToMany(mappedBy = "sewadar", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SewadarLanguage> languages = new ArrayList<>();
}

