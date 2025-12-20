package com.rssb.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ActionResponse entity - sewadar's response to an action.
 */
@Entity
@Table(name = "action_responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActionResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "action_id", nullable = false)
    private Action action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sewadar_id", nullable = false)
    private Sewadar sewadar;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData; // JSON or text response

    @Column(name = "status", length = 50)
    @Builder.Default
    private String status = "PENDING"; // PENDING, COMPLETED

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "notes", length = 500)
    private String notes;
}

