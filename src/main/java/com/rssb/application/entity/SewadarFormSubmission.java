package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores form submissions from sewadars for program travel details.
 */
@Entity
@Table(name = "sewadar_form_submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SewadarFormSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sewadar_id", nullable = false, referencedColumnName = "zonal_id")
    private Sewadar sewadar;

    // Form fields (name removed - available from sewadar relationship)
    @Column(name = "starting_date_time_from_home")
    private LocalDateTime startingDateTimeFromHome;

    @Column(name = "reaching_date_time_to_home")
    private LocalDateTime reachingDateTimeToHome;

    // Journey details
    @Column(name = "onward_train_flight_date_time")
    private LocalDateTime onwardTrainFlightDateTime;

    @Column(name = "onward_train_flight_no", length = 100)
    private String onwardTrainFlightNo;

    @Column(name = "return_train_flight_date_time")
    private LocalDateTime returnTrainFlightDateTime;

    @Column(name = "return_train_flight_no", length = 100)
    private String returnTrainFlightNo;

    // Stay details
    @Column(name = "stay_in_hotel", length = 500)
    private String stayInHotel;

    @Column(name = "stay_in_pandal", length = 500)
    private String stayInPandal;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

