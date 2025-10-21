package com.gpis.marketplace_link.services.publications;

import com.gpis.marketplace_link.dto.incidence.RequestSystemReport;
import com.gpis.marketplace_link.dto.publication.request.PublicationCreateRequest;
import com.gpis.marketplace_link.dto.publication.request.PublicationUpdateRequest;
import com.gpis.marketplace_link.dto.publication.response.PublicationResponse;
import com.gpis.marketplace_link.dto.publication.response.PublicationSummaryResponse;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.entities.PublicationImage;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.exceptions.business.publications.DangerousContentException;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationCanNotDeleteException;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationNotFoundException;
import com.gpis.marketplace_link.exceptions.business.publications.UserIsNotVendorException;
import com.gpis.marketplace_link.mappers.PublicationMapper;
import com.gpis.marketplace_link.repositories.PublicationRepository;
import com.gpis.marketplace_link.repositories.UserRepository;
import com.gpis.marketplace_link.repositories.CategoryRepository;
import com.gpis.marketplace_link.services.incidence.IncidenceService;
import com.gpis.marketplace_link.services.incidence.IncidenceServiceImp;
import com.gpis.marketplace_link.services.publications.valueObjects.DangerousWordMatch;
import com.gpis.marketplace_link.specifications.PublicationSpecifications;
import com.gpis.marketplace_link.enums.PublicationStatus;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

@Service
public class PublicationService {

    private final PublicationRepository repository;
    private final PublicationMapper mapper;
    private final FileStorageService fileStorageService;
    private final DangerousContentDetectedService dangerousContentDetectedService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ImageValidationService imageValidationService;
    private final IncidenceService incidenceService;
    private static final Logger logger = LoggerFactory.getLogger(PublicationService.class);

    public PublicationService(PublicationRepository repository, PublicationMapper mapper, FileStorageService fileStorageService, UserRepository userRepository, CategoryRepository categoryRepository, ImageValidationService imageValidationService, IncidenceServiceImp incidenceServiceImp, DangerousContentDetectedService dangerousContentDetectedService) {
        this.repository = repository;
        this.mapper = mapper;
        this.fileStorageService = fileStorageService;
        this.incidenceService = incidenceServiceImp;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.imageValidationService = imageValidationService;
        this.dangerousContentDetectedService = dangerousContentDetectedService;
    }

    @Transactional(readOnly = true)
    public Page<PublicationSummaryResponse> getAll(Pageable pageable, List<Long> categoryIds, BigDecimal minPrice, BigDecimal maxPrice, Double lat, Double lon, Double distanceKm) {

        Specification<Publication> spec =
                PublicationSpecifications.statusIs(PublicationStatus.VISIBLE.getValue())
                        .and(PublicationSpecifications.notDeleted())
                        .and(PublicationSpecifications.notSuspended())
                        .and(PublicationSpecifications.hasAnyCategory(categoryIds))
                        .and(PublicationSpecifications.priceBetween(minPrice, maxPrice))
                        .and(PublicationSpecifications.withinDistance(lat, lon, distanceKm));

        Page<Publication> publications = repository.findAll(spec, pageable);

        return publications.map(mapper::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public Page<PublicationSummaryResponse> getAllByVendor(Pageable pageable, List<Long> categoryIds, Long vendorId) {

        this.validateUserAndRole(vendorId);

        Specification<Publication> spec =
                PublicationSpecifications.vendorIs(vendorId)
                        .and(PublicationSpecifications.statusIs(PublicationStatus.VISIBLE.getValue()))
                        .and(PublicationSpecifications.notDeleted())
                        .and(PublicationSpecifications.hasAnyCategory(categoryIds));

        Page<Publication> publications = repository.findAll(spec, pageable);

        return publications.map(mapper::toSummaryResponse);
    }


    public PublicationResponse getById(Long id) {

        Publication publication = this.validatePublication(id);

        return mapper.toResponse(publication);

    }


    @Transactional(noRollbackFor = DangerousContentException.class)
    public PublicationResponse create(PublicationCreateRequest request) {

        validateUserAndRole(request.vendorId());

        imageValidationService.validateImages(request.images());

        List<String> imagesNames = request.images().stream()
                .map(fileStorageService::storeFile)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .toList();

        Publication publication = mapper.toEntity(request);

        publication.setVendor(userRepository.getReferenceById(request.vendorId()));
        publication.setCategory(categoryRepository.getReferenceById(request.categoryId()));

        publication.setImages(new ArrayList<>());

        for (String path : imagesNames) {
            PublicationImage img = new PublicationImage();
            img.setPath(path);
            img.setPublication(publication);
            publication.getImages().add(img);
        }

        validateDangerousContent(publication);

        publication.setStatus(PublicationStatus.VISIBLE);
        Publication saved = repository.save(publication);


        return mapper.toResponse(saved);
    }

    @Transactional(noRollbackFor = DangerousContentException.class)
    public PublicationResponse update(Long id, PublicationUpdateRequest request) {


        validateUserAndRole(request.vendorId());

        Publication publication = this.validatePublication(id);


        mapper.updateFromRequest(publication, request);

        publication.setVendor(userRepository.getReferenceById(request.vendorId()));
        publication.setCategory(categoryRepository.getReferenceById(request.categoryId()));

        Map<String, PublicationImage> existingMap = publication.getImages().stream()
                .collect(Collectors.toMap(PublicationImage::getPath, img -> img));


        List<PublicationImage> imagesToRemove = publication.getImages().stream()
                .filter(img -> request.images().stream()
                        .noneMatch(f -> f.getOriginalFilename().equals(img.getPath())))
                .toList();

        for (PublicationImage img : imagesToRemove) {
            publication.getImages().remove(img);
            fileStorageService.deleteFile(img.getPath());
        }

        List<MultipartFile> newFiles = request.images().stream()
                .filter(f -> !existingMap.containsKey(f.getOriginalFilename()))
                .toList();

        for (MultipartFile file : newFiles) {
            String path = fileStorageService.storeFile(file);
            PublicationImage img = new PublicationImage();
            img.setPath(path);
            img.setPublication(publication);
            publication.getImages().add(img);
        }

        validateDangerousContent(publication);

        Publication saved = repository.save(publication);

        return mapper.toResponse(saved);

    }

    public void delete(Long id) {

        Publication publication = this.repository.findById(id)
                .orElseThrow(() -> new PublicationNotFoundException(
                        "Publicación con id " + id + " no encontrada"));

        if (publication.getStatus() == PublicationStatus.UNDER_REVIEW ||
                publication.getStatus() == PublicationStatus.BLOCKED) {

            throw new PublicationCanNotDeleteException("No se puede eliminar la publicación debido a que se encuentra en revision o bloqueada");

        }

        publication.setDeletedAt(LocalDateTime.now());

        List<PublicationImage> images = publication.getImages();

        for (PublicationImage img : images) {
            fileStorageService.deleteFile(img.getPath());
        }

        this.repository.save(publication);



    }


    public void suspendedPublicationsOlderThan(Integer limit) {

        LocalDateTime now = LocalDateTime.now();

        List<Publication> publications = this.repository.findAll();

        List<Publication> toSuspend = new ArrayList<>();
        for (Publication pub : publications) {
            if (pub.getPublicationDate().plusDays(limit).isBefore(now) && !pub.isSuspended()) {
                pub.setSuspended(true);
                toSuspend.add(pub);
                logger.info("Se ha suspendido la publicación con id: {}", pub.getId());
            }
        }
        repository.saveAll(toSuspend);

    }


    public Publication validatePublication(Long id) {

        Specification<Publication> spec =
                PublicationSpecifications.idIs(id)
                        .and(PublicationSpecifications.statusIs(PublicationStatus.VISIBLE.getValue()))
                        .and(PublicationSpecifications.notDeleted())
                        .and(PublicationSpecifications.notSuspended());

        return repository.findOne(spec)
                .orElseThrow(() -> new PublicationNotFoundException(
                        "Publicación con id " + id + " no encontrada"
                ));

    }


    private void validateDangerousContent(Publication publication) {
        if (dangerousContentDetectedService.containsDangerousContent(publication.getName())
                || dangerousContentDetectedService.containsDangerousContent(publication.getDescription())) {

            publication.setStatus(PublicationStatus.UNDER_REVIEW);
            Publication saved = repository.save(publication);

            List<DangerousWordMatch> dangerousWordsDetected = dangerousContentDetectedService.findDangerousWords(
                    publication.getName() + " " + publication.getDescription()
            );

            this.reportPublicationForDangerousContent(saved, dangerousWordsDetected);

            throw new DangerousContentException(
                    "Se ha detectado que su publicación contiene contenido peligroso, por lo que ha sido enviada a revisión, si sospecha que se ha cometido un error por favor realice una apelacion, esta le llegara a su correo electrónico."
            );
        }
    }


    private void reportPublicationForDangerousContent(Publication publication, List<DangerousWordMatch> dangerousWordsDetected) {
        RequestSystemReport requestSystemReport = new RequestSystemReport();
        requestSystemReport.setPublicationId(publication.getId());
        requestSystemReport.setReason("Contenido peligroso detectado");
        requestSystemReport.setComment(
                "Se han detectado las siguientes palabras: " +
                        dangerousWordsDetected.stream()
                                .map(dw -> dw.wordInText() + " (" + dw.patternMatched() + ")")
                                .distinct()
                                .collect(Collectors.joining(", "))
        );
        incidenceService.reportBySystem(requestSystemReport);
    }

    private void validateUserAndRole(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.getRoles().stream().noneMatch(r -> r.getName().equals("ROLE_SELLER"))) {
            throw new UserIsNotVendorException("El usuario no es vendedor");
        }
    }


}
