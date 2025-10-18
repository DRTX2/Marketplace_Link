package com.gpis.marketplace_link.services;

import com.gpis.marketplace_link.dto.publication.response.PublicationResponse;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.mappers.PublicationMapper;
import com.gpis.marketplace_link.repositories.PublicationRepository;
import com.gpis.marketplace_link.specifications.PublicationSpecifications;
import com.gpis.marketplace_link.enums.PublicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PublicationService {

    private final PublicationRepository repository;
    private final PublicationMapper mapper;

    public PublicationService(PublicationRepository repository , PublicationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public Page<PublicationResponse> getAll(Pageable pageable, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Double lat, Double lon, Double distanceKm) {

        Specification<Publication> spec =
                PublicationSpecifications.statusIs(PublicationStatus.VISIBLE.getValue())
                .and(PublicationSpecifications.notDeleted())
                .and(PublicationSpecifications.notSuspended())
                .and(PublicationSpecifications.hasCategory(categoryId))
                .and(PublicationSpecifications.priceBetween(minPrice, maxPrice))
                .and(PublicationSpecifications.withinDistance(lat, lon, distanceKm));

        Page<Publication> publications = repository.findAll(spec, pageable);

        return publications.map(mapper::toResponse);
    }

}
