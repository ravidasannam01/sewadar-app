package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "dept", length = 100)
    private String dept;

    @Column(name = "mobile", length = 20)
    private String mobile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @OneToMany(mappedBy = "attendedBy", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Schedule> schedules = new ArrayList<>();
}

