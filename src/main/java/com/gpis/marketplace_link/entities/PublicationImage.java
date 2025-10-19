package com.gpis.marketplace_link.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "publication_images")
@Data
public class PublicationImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String path;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_id", nullable = false)
    @JsonIgnore
    private Publication publication;
}
