package com.gpis.marketplace_link.services;

import com.gpis.marketplace_link.dto.publication.response.PublicationResponse;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.mappers.PublicationMapper;
import com.gpis.marketplace_link.repository.PublicationRepository;
import com.gpis.marketplace_link.specifications.PublicationSpecifications;
import com.gpis.marketplace_link.valueObjects.PublicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PublicationService {

    private final PublicationRepository repository;
    private final GeocodingService geocodingService;
    private final PublicationMapper mapper;

    public PublicationService(PublicationRepository repository , GeocodingService geocodingService, PublicationMapper mapper) {
        this.repository = repository;
        this.geocodingService = geocodingService;
        this.mapper = mapper;
    }

    public Page<PublicationResponse> getAll(Pageable pageable, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Double lat, Double lon, Double distanceKm) {

        Specification<Publication> spec =
                PublicationSpecifications.statusIs(PublicationStatus.VISIBLE.getValue())
                .and(PublicationSpecifications.notDeleted())
                .and(PublicationSpecifications.notSuspended())
                .and(PublicationSpecifications.hasCategory(categoryId))
                .and(PublicationSpecifications.priceBetween(minPrice, maxPrice))
                .and(PublicationSpecifications.withinDistance(lat, lon, distanceKm));

        Page<Publication> publications = repository.findAll(spec, pageable);

        return publications.map(pub -> {
            String city = null;
            String country = null;
            if (pub.getLocation() != null) {
                var locationInfoOpt = geocodingService.reverseGeocode(pub.getLocation().getY(), pub.getLocation().getX());
                if (locationInfoOpt.isPresent()) {
                    var locationInfo = locationInfoOpt.get();
                    city = locationInfo.city();
                    country = locationInfo.country();
                }
            }
            return mapper.toResponse(pub, city, country);
        });
    }

}
