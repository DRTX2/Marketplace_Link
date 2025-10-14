package com.gpis.marketplace_link.services;

import com.gpis.marketplace_link.dto.publication.response.PublicationResponse;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.mappers.PublicationMapper;
import com.gpis.marketplace_link.repository.PublicationRepository;
import com.gpis.marketplace_link.specifications.PublicationSpecifications;
import com.gpis.marketplace_link.valueObjects.PublicationStatus;
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


        // Obtener lista ordenada para respetar sort del pageable
        List<Publication> all = repository.findAll(spec, pageable.getSort());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<Publication> pageSlice = start <= end ? all.subList(start, end) : List.of();
        Page<Publication> publications = new PageImpl<>(pageSlice, pageable, all.size());

        return publications.map(mapper::toResponse);
    }

}
