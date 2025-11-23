package com.gpis.marketplace_link.entities;

import com.gpis.marketplace_link.enums.AppealDecision;
import com.gpis.marketplace_link.enums.AppealStatus;
import com.gpis.marketplace_link.enums.IncidenceDecision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "appeals")
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne()
    @JoinColumn(name = "incidence_id", nullable = false)
    private Incidence incidence;

    @ManyToOne()
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private AppealStatus status = AppealStatus.PENDING;

    @ManyToOne()
    @JoinColumn(name = "new_moderator_id", nullable = false)
    private User newModerator;

    @Column(name = "final_decision")
    @Enumerated(EnumType.STRING)
    private AppealDecision finalDecision;

    @Column(name = "final_decision_at")
    private LocalDateTime finalDecisionAt;

    @PrePersist
    protected void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (finalDecision == null) {
            finalDecision = AppealDecision.PENDING;
        }
    }
}
