package com.gpis.marketplace_link.entities.lights;

import com.gpis.marketplace_link.enums.PublicationStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "publications")
public class PublicationLight {

    @Id
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicationStatus status;

    public void setUnderReview() {
        this.status = PublicationStatus.UNDER_REVIEW;
    }

    public void setVisible() {
        this.status = PublicationStatus.VISIBLE;
    }

    public void setBlocked() {
        this.status = PublicationStatus.BLOCKED;
    }

}
