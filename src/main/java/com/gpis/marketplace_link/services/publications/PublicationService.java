package com.gpis.marketplace_link.services.publications;

import com.gpis.marketplace_link.dto.incidence.RequestSystemReport;
import com.gpis.marketplace_link.dto.publication.request.PublicationCreateRequest;
import com.gpis.marketplace_link.dto.publication.request.PublicationUpdateRequest;
import com.gpis.marketplace_link.dto.publication.response.PublicationResponse;
import com.gpis.marketplace_link.dto.publication.response.PublicationSummaryResponse;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.entities.PublicationImage;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.enums.PublicationType;
import com.gpis.marketplace_link.exceptions.business.publications.DangerousContentException;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationCanNotDeleteException;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationNotFoundException;
import com.gpis.marketplace_link.exceptions.business.publications.UserIsNotVendorException;
import com.gpis.marketplace_link.mappers.PublicationMapper;
import com.gpis.marketplace_link.repositories.PublicationRepository;
import com.gpis.marketplace_link.repositories.UserRepository;
import com.gpis.marketplace_link.repositories.CategoryRepository;
import com.gpis.marketplace_link.repositories.ReportRepository;
import com.gpis.marketplace_link.security.service.SecurityService;
import com.gpis.marketplace_link.entities.Report;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.Optional;

import org.slf4j.Logger;
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
    private final FavoritePublicationService favoritePublicationService;
    private final SecurityService securityService;
    private final ReportRepository reportRepository;
    private static final Logger logger = LoggerFactory.getLogger(PublicationService.class);

    public PublicationService(PublicationRepository repository, PublicationMapper mapper,
            FileStorageService fileStorageService, UserRepository userRepository, CategoryRepository categoryRepository,
            ImageValidationService imageValidationService, IncidenceServiceImp incidenceServiceImp,
            DangerousContentDetectedService dangerousContentDetectedService,
            FavoritePublicationService favoritePublicationService, SecurityService securityService,
            ReportRepository reportRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.fileStorageService = fileStorageService;
        this.incidenceService = incidenceServiceImp;
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.imageValidationService = imageValidationService;
        this.dangerousContentDetectedService = dangerousContentDetectedService;
        this.favoritePublicationService = favoritePublicationService;
        this.securityService = securityService;
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public Page<PublicationSummaryResponse> getAll(Pageable pageable, List<Long> categoryIds, BigDecimal minPrice,
            BigDecimal maxPrice, Double lat, Double lon, Double distanceKm) {

        Specification<Publication> spec = PublicationSpecifications.statusIs(PublicationStatus.VISIBLE.getValue())
                .and(PublicationSpecifications.notDeleted())
                .and(PublicationSpecifications.notSuspended())
                .and(PublicationSpecifications.hasAnyCategory(categoryIds))
                .and(PublicationSpecifications.priceBetween(minPrice, maxPrice))
                .and(PublicationSpecifications.withinDistance(lat, lon, distanceKm))
                .and(PublicationSpecifications.vendorAccountStatusIsActive());

        Page<Publication> publications = repository.findAll(spec, pageable);

        return publications.map(pub -> {
            PublicationSummaryResponse baseResponse = mapper.toSummaryResponse(pub);
            Boolean canReport = calculateCanReport(pub);
            return new PublicationSummaryResponse(
                    baseResponse.id(),
                    baseResponse.type(),
                    baseResponse.name(),
                    baseResponse.price(),
                    baseResponse.availability(),
                    baseResponse.publicationDate(),
                    baseResponse.image(),
                    canReport);
        });
    }

    @Transactional(readOnly = true)
    public Page<PublicationSummaryResponse> getAllByVendor(Pageable pageable, List<Long> categoryIds, Long vendorId) {

        this.validateUserAndRole(vendorId);

        Specification<Publication> spec = PublicationSpecifications.vendorIs(vendorId)
                .and(PublicationSpecifications.statusIs(PublicationStatus.VISIBLE.getValue()))
                .and(PublicationSpecifications.notDeleted())
                .and(PublicationSpecifications.hasAnyCategory(categoryIds));

        Page<Publication> publications = repository.findAll(spec, pageable);

        return publications.map(pub -> {
            PublicationSummaryResponse baseResponse = mapper.toSummaryResponse(pub);
            Boolean canReport = calculateCanReport(pub);
            return new PublicationSummaryResponse(
                    baseResponse.id(),
                    baseResponse.type(),
                    baseResponse.name(),
                    baseResponse.price(),
                    baseResponse.availability(),
                    baseResponse.publicationDate(),
                    baseResponse.image(),
                    canReport);
        });
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

        imageValidationService.validateImages(request.images());

        validateUserAndRole(request.vendorId());

        Publication publication = this.validatePublication(id);

        mapper.updateFromRequest(publication, request);

        publication.setVendor(userRepository.getReferenceById(request.vendorId()));
        publication.setCategory(categoryRepository.getReferenceById(request.categoryId()));
        if (request.workingHours() != null) {
            publication.setWorkingHours(request.workingHours());
        }
        publication.setType(publication.getWorkingHours() != null ? PublicationType.SERVICE : PublicationType.PRODUCT);

        // 1. Obtener URLs de imágenes que se deben MANTENER (vienen del frontend)
        List<String> imagesToKeep = request.existingImageUrls() != null ? request.existingImageUrls()
                : new ArrayList<>();

        // 2. Identificar imágenes que están en la BD pero NO en la lista de mantener ->
        // ELIMINAR
        List<PublicationImage> imagesToRemove = publication.getImages().stream()
                .filter(img -> !imagesToKeep.contains(img.getPath())) // Si no está en la lista de keep, se borra
                .toList();

        // 3. Eliminar imágenes (BD y Storage)
        for (PublicationImage img : imagesToRemove) {
            publication.getImages().remove(img);
            fileStorageService.deleteFile(img.getPath());
        }

        // 4. Procesar NUEVAS imágenes (archivos subidos)
        if (request.images() != null) {
            for (MultipartFile file : request.images()) {
                if (file.isEmpty())
                    continue;

                String path = fileStorageService.storeFile(file);
                PublicationImage img = new PublicationImage();
                img.setPath(path);
                img.setPublication(publication);
                publication.getImages().add(img);
            }
        }

        validateDangerousContent(publication);

        Publication saved = repository.save(publication);

        return mapper.toResponse(saved);

    }

    @Transactional
    public void blockPublicationsByVendor(Long vendorId) {
        List<Publication> publications = repository.findAllByVendorIdAndDeletedAtIsNull(vendorId);
        List<Publication> toUpdate = new ArrayList<>();
        for (Publication publication : publications) {
            if (publication.getStatus() != PublicationStatus.BLOCKED) {
                publication.setPreviousStatus(publication.getStatus());
                publication.setStatus(PublicationStatus.BLOCKED);
            }
            favoritePublicationService.removeFavoritesByPublicationId(publication.getId());
            toUpdate.add(publication);
        }
        if (!toUpdate.isEmpty()) {
            repository.saveAll(toUpdate);
        }
    }

    @Transactional
    public void restorePublicationsByVendor(Long vendorId) {
        List<Publication> publications = repository.findAllByVendorIdAndDeletedAtIsNull(vendorId);
        List<Publication> toUpdate = new ArrayList<>();
        for (Publication publication : publications) {
            if (publication.getStatus() == PublicationStatus.BLOCKED) {
                PublicationStatus previous = publication.getPreviousStatus() != null
                        ? publication.getPreviousStatus()
                        : PublicationStatus.VISIBLE;
                publication.setStatus(previous);
                publication.setPreviousStatus(null);
                favoritePublicationService.restoreFavoritesByPublicationId(publication.getId());
                toUpdate.add(publication);
            }
        }
        if (!toUpdate.isEmpty()) {
            repository.saveAll(toUpdate);
        }
    }

    @Transactional
    public void delete(Long id) {
        Publication publication = repository.findByIdWithImages(id)
                .orElseThrow(() -> new PublicationNotFoundException("Publication con id " + id + " no encontrada"));

        if (publication.getStatus() == PublicationStatus.UNDER_REVIEW
                || publication.getStatus() == PublicationStatus.BLOCKED) {
            throw new PublicationCanNotDeleteException(
                    "No se puede eliminar la publicación debido a que se encuentra en revision o bloqueada");
        }

        favoritePublicationService.removeFavoritesByPublicationId(publication.getId());

        List<String> imagePaths = publication.getImages().stream()
                .map(PublicationImage::getPath)
                .filter(Objects::nonNull)
                .toList();

        publication.setDeletedAt(LocalDateTime.now());
        publication.setPreviousStatus(null);

        repository.save(publication);

        for (String path : imagePaths)
            fileStorageService.deleteFile(path);

    }

    public void suspendedPublicationsOlderThan(Integer limit) {

        LocalDateTime now = LocalDateTime.now();

        List<Publication> publications = this.repository.findAll();

        List<Publication> toSuspend = new ArrayList<>();
        for (Publication pub : publications) {
            if (pub.getPublicationDate().plusSeconds(limit).isBefore(now) && !pub.isSuspended()) {
                pub.setSuspended(true);
                toSuspend.add(pub);
                logger.info("Se ha suspendido la publicación con id: {}", pub.getId());
            }
        }
        repository.saveAll(toSuspend);

    }

    public Publication validatePublication(Long id) {

        Specification<Publication> spec = PublicationSpecifications.idIs(id)
                .and(PublicationSpecifications.statusIs(PublicationStatus.VISIBLE.getValue()))
                .and(PublicationSpecifications.notDeleted())
                .and(PublicationSpecifications.notSuspended())
                .and(PublicationSpecifications.vendorAccountStatusIsActive());

        return repository.findOne(spec)
                .orElseThrow(() -> new PublicationNotFoundException(
                        "Publicación con id " + id + " no encontrada"));

    }

    private void validateDangerousContent(Publication publication) {
        if (dangerousContentDetectedService.containsDangerousContent(publication.getName())
                || dangerousContentDetectedService.containsDangerousContent(publication.getDescription())) {

            publication.setStatus(PublicationStatus.UNDER_REVIEW);
            Publication saved = repository.save(publication);

            List<DangerousWordMatch> dangerousWordsDetected = dangerousContentDetectedService.findDangerousWords(
                    publication.getName() + " " + publication.getDescription());

            this.reportPublicationForDangerousContent(saved, dangerousWordsDetected);

            throw new DangerousContentException(
                    "Se ha detectado que su publicación contiene contenido peligroso, por lo que ha sido enviada a revisión, si sospecha que se ha cometido un error por favor realice una apelacion, esta le llegara a su correo electrónico.");
        }
    }

    private void reportPublicationForDangerousContent(Publication publication,
            List<DangerousWordMatch> dangerousWordsDetected) {
        RequestSystemReport requestSystemReport = new RequestSystemReport();
        requestSystemReport.setPublicationId(publication.getId());
        requestSystemReport.setReason("Contenido peligroso detectado");
        requestSystemReport.setComment(
                "Se han detectado las siguientes palabras: " +
                        dangerousWordsDetected.stream()
                                .map(dw -> dw.wordInText() + " (" + dw.patternMatched() + ")")
                                .distinct()
                                .collect(Collectors.joining(", ")));
        incidenceService.reportBySystem(requestSystemReport);
    }

    private void validateUserAndRole(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.getRoles().stream().noneMatch(r -> r.getName().equals("ROLE_SELLER"))) {
            throw new UserIsNotVendorException("El usuario no es vendedor");
        }
    }

    private Boolean calculateCanReport(Publication publication) {
        try {
            Long currentUserId = securityService.getCurrentUserId();

            if (publication.getVendor().getId().equals(currentUserId)) {
                return false;
            }

            Optional<Report> lastReport = reportRepository.findLastReportByReporterIdAndPublicationId(
                    currentUserId, publication.getId());

            if (lastReport.isPresent()) {
                LocalDateTime lastReportTime = lastReport.get().getCreatedAt();
                LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
                if (lastReportTime.isAfter(twentyFourHoursAgo)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
