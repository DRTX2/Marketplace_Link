package com.gpis.marketplace_link.entities;

import com.gpis.marketplace_link.enums.PublicationAvailable;
import com.gpis.marketplace_link.enums.PublicationStatus;
import com.gpis.marketplace_link.enums.PublicationType;
import jakarta.persistence.*;
import lombok.Data;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "publications")
@Data

public class Publication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 20)
    private String code;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PublicationType type;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PublicationAvailable availability;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PublicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private PublicationStatus previousStatus;

    @Column(name = "publication_date")
    private LocalDateTime publicationDate;

    @Column(nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private boolean suspended = false;

    private String workingHours;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private User vendor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "publication", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PublicationImage> images;

    public void setUnderReview() {
        this.status = PublicationStatus.UNDER_REVIEW;
    }

    public void setVisible() {
        this.status = PublicationStatus.VISIBLE;
    }

    public void setBlocked() {
        this.status = PublicationStatus.BLOCKED;
    }

    @PrePersist
    public void prePersist() {
        this.publicationDate = LocalDateTime.now();

        String raw = UUID.randomUUID().toString().replace("-", "");
        this.code = raw.substring(0, 20);
        this.type = this.workingHours != null ? PublicationType.SERVICE : PublicationType.PRODUCT;
        this.availability= PublicationAvailable.AVAILABLE;
    }

    @PreUpdate
    public void preUpdate()
    {
        this.type = this.workingHours != null ? PublicationType.SERVICE : PublicationType.PRODUCT;
    }
}
