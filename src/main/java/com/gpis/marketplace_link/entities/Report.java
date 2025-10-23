package com.gpis.marketplace_link.entities;

import com.gpis.marketplace_link.enums.ReportSource;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Entity()
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne()
    @JoinColumn(name = "incidence_id")
    private Incidence incidence;

    @ManyToOne()
    @JoinColumn(name = "reporter_id", nullable = true)
    private User reporter; // usuario que reporta

    @Column(name = "reason", nullable = false, length = 100)
    private String reason; // motivo del reporte

    @Column(name = "comment")
    private String comment; // comentario adicional (opcional)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // se setea al crear el reporte

    @Enumerated(EnumType.STRING)
    private ReportSource source; // fuente usuario o sistema

    @PrePersist()
    public void prePersist() {
        createdAt = LocalDateTime.now();
        if (source == null) {
            source = ReportSource.USER;
        }
    }
}
