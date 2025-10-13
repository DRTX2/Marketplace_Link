package com.gpis.marketplace_link.entities;

import com.gpis.marketplace_link.enums.Decision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity(name = "appeals")
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne()
    @JoinColumn(name = "incidence_id")
    private Incidence incidence;

    @ManyToOne()
    @JoinColumn(name = "seller_id")
    private User seller;

    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne()
    @JoinColumn(name = "new_moderator_id")
    private User newModerator;

    @Column(name = "final_decision")
    @Enumerated(EnumType.STRING)
    private Decision finalDecision;

    @Column(name = "final_decision_at")
    private LocalDateTime finalDecisionAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
