package com.gpis.marketplace_link.services.publications;

import com.gpis.marketplace_link.dto.publication.request.PublicationCreateRequest;
import com.gpis.marketplace_link.dto.publication.response.PublicationResponse;
import com.gpis.marketplace_link.dto.publication.response.PublicationSummaryResponse;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.entities.PublicationImage;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationNotFoundException;
import com.gpis.marketplace_link.mappers.PublicationMapper;
import com.gpis.marketplace_link.repositories.PublicationImageRepository;
import com.gpis.marketplace_link.repositories.PublicationRepository;
import com.gpis.marketplace_link.repositories.UserRepository;
import com.gpis.marketplace_link.repositories.CategoryRepository;
import com.gpis.marketplace_link.specifications.PublicationSpecifications;
import com.gpis.marketplace_link.enums.PublicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class PublicationService {

    private final PublicationRepository repository;
    private final PublicationMapper mapper;
    private final FileStorageService fileStorageService;
    private final PublicationImageRepository publicationImageRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ImageValidationService imageValidationService;

    public PublicationService(PublicationRepository repository , PublicationMapper mapper,FileStorageService fileStorageService, PublicationImageRepository publicationImageRepository, UserRepository userRepository, CategoryRepository categoryRepository, ImageValidationService imageValidationService) {
        this.repository = repository;
        this.mapper = mapper;
        this.fileStorageService = fileStorageService;
        this.publicationImageRepository = publicationImageRepository;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.imageValidationService = imageValidationService;
    }

    @Transactional(readOnly = true)
    public Page<PublicationSummaryResponse> getAll(Pageable pageable, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Double lat, Double lon, Double distanceKm) {

        Specification<Publication> spec =
                PublicationSpecifications.statusIs(PublicationStatus.VISIBLE.getValue())
                .and(PublicationSpecifications.notDeleted())
                .and(PublicationSpecifications.notSuspended())
                .and(PublicationSpecifications.hasCategory(categoryId))
                .and(PublicationSpecifications.priceBetween(minPrice, maxPrice))
                .and(PublicationSpecifications.withinDistance(lat, lon, distanceKm));

        Page<Publication> publications = repository.findAll(spec, pageable);

        return publications.map(mapper::toSummaryResponse);
    }

    public PublicationResponse getById(Long id){

        Specification<Publication> spec =
                PublicationSpecifications.idIs(id)
                        .and(PublicationSpecifications.statusIs(PublicationStatus.VISIBLE.getValue()))
                        .and(PublicationSpecifications.notDeleted())
                        .and(PublicationSpecifications.notSuspended());

        Publication publication = repository.findOne(spec)
                .orElseThrow(() -> new PublicationNotFoundException(
                        "Publicación con id " + id + " no encontrada"
                ));

        return mapper.toResponse(publication);


    }

    @Transactional
    public PublicationResponse create(PublicationCreateRequest request){


        imageValidationService.validateImages(request.images());

        List<String> imagesNames = request.images().stream()
                .map(fileStorageService::storeFile)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .toList();

        Publication publication = mapper.toEntity(request);

            publication.setVendor(userRepository.getReferenceById(request.vendorId()));
            publication.setCategory(categoryRepository.getReferenceById(request.categoryId()));

            // Inicializar la lista de imágenes ANTES de agregar elementos
            publication.setImages(new ArrayList<>());

            // Agregar imágenes con la relación bidireccional configurada correctamente
        for (String path : imagesNames) {
            PublicationImage img = new PublicationImage();
            img.setPath(path);
            img.setPublication(publication);
            publication.getImages().add(img);
        }

        publication.setStatus(PublicationStatus.VISIBLE);
        Publication saved = repository.save(publication);

        return mapper.toResponse(saved);
    }



}
