package com.gpis.marketplace_link.entities.lights;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class ReportLight {

    @Id
    private Long id;

    private String reason;

    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incidence_id")
    private IncidenceLight incidence;

}
