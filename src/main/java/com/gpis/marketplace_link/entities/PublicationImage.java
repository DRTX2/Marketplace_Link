package com.gpis.marketplace_link.entities;

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

    @ManyToOne
    @JoinColumn(name = "publication_id")
    private Publication publication;


}

