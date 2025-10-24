package com.gpis.marketplace_link.entities.lights;

import com.gpis.marketplace_link.enums.IncidenceDecision;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entidad ligera de Incidence usada para operaciones nativas
 * que no requieren cargar relaciones (User, Publication).
 */
@Getter
@Setter
@Entity
@Table(name = "incidences")
public class IncidenceLight {

    @Id
    private Long id;

    @Column(name = "public_ui")
    private UUID publicUi;

    @Enumerated(EnumType.STRING)
    private IncidenceStatus status;

    @Enumerated(EnumType.STRING)
    private IncidenceDecision decision;

    @Column(name = "moderator_comment", length = 500)
    private String moderatorComment;

    @Column(name = "auto_closed")
    private Boolean autoClosed;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "incidence", fetch = FetchType.LAZY)
    private List<ReportLight> reports;

}
