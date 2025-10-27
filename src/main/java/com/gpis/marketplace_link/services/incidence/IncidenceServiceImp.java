package com.gpis.marketplace_link.services.incidence;

import com.gpis.marketplace_link.dto.Messages;
import com.gpis.marketplace_link.dto.incidence.AppealResponse;
import com.gpis.marketplace_link.dto.incidence.*;
import com.gpis.marketplace_link.dto.incidence.projections.IncidenceDetailsProjection;
import com.gpis.marketplace_link.dto.incidence.projections.IncidenceStatsProjection;
import com.gpis.marketplace_link.dto.incidence.projections.UserIdProjection;
import com.gpis.marketplace_link.dto.incidence.projections.VendorIdProjection;
import com.gpis.marketplace_link.entities.*;
import com.gpis.marketplace_link.enums.*;
import com.gpis.marketplace_link.exceptions.business.AccessDeniedException;
import com.gpis.marketplace_link.exceptions.business.incidences.IncidenceNotAppealableException;
import com.gpis.marketplace_link.exceptions.business.incidences.*;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationNotFoundException;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationUnderReviewException;
import com.gpis.marketplace_link.exceptions.business.users.ModeratorNotFoundException;
import com.gpis.marketplace_link.exceptions.business.users.ReporterNotFoundException;
import com.gpis.marketplace_link.exceptions.business.users.UserNotFoundException;
import com.gpis.marketplace_link.repositories.*;
import com.gpis.marketplace_link.security.service.SecurityService;
import com.gpis.marketplace_link.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidenceServiceImp implements IncidenceService {

    @Value("${FRONTEND_URL:http://localhost:3000}")
    private String frontendUrl;

    private final NotificationService notificationService;
    private final SecurityService securityService;
    private final IncidenceRepository incidenceRepository;
    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final AppealRepository appealRepository;

    private static final int REPORT_THRESHOLD = 3;
    private static final String SYSTEM_USERNAME = "system_user";

    @Override
    public IncidenceStatsResponse fetchStatsByUserId(Long id) {

        IncidenceStatsProjection proj = this.incidenceRepository.fetchStatsById(id);

        IncidenceStatsResponse response = new IncidenceStatsResponse();
        response.setResolved(proj.getResolved());
        response.setTotal(proj.getTotal());
        response.setAppealed(proj.getAppealed());
        response.setUnderReview(proj.getUnderReview());

        return response;
    }

    @Transactional
    @Override
    public void autoclose() {
        int hours = 3;
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(hours);
        incidenceRepository.bulkAutoClose(cutoff);
    }

    @Transactional()
    @Override
    public ReportResponse reportByUser(RequestUserReport req) {
        List<IncidenceStatus> status = List.of(
                IncidenceStatus.OPEN,
                IncidenceStatus.PENDING_REVIEW,
                IncidenceStatus.UNDER_REVIEW,
                IncidenceStatus.APPEALED);

        Long reporterId = securityService.getCurrentUserId();
        Long publicationId = req.getPublicationId();

        // Obtener entidades base
        Publication pub = publicationRepository.findById(publicationId).orElseThrow(() -> new PublicationNotFoundException(Messages.PUBLICATION_NOT_FOUND + publicationId));
        User reporter = userRepository.findById(reporterId).orElseThrow(() -> new ReporterNotFoundException(Messages.REPORTER_NOT_FOUND + reporterId));

        // Validar si el usuario intenta reportar su propia publicación
        if (pub.getVendor().getId().equals(reporter.getId())) {
            throw new IncidenceNotAllowedToReportOwnPublicationException("No puedes reportar tu propia publicación.");
        }

        // Crear o actualizar incidencia
        Optional<Incidence> inc = incidenceRepository.findByPublicationIdAndStatusIn(publicationId, status);
        if (inc.isEmpty()) {
            return handleNewIncidence(pub, reporter, req);
        } else {
            return handleExistingIncidence(inc.get(), pub, reporter, req);
        }
    }

    private ReportResponse handleNewIncidence(Publication pub, User reporter, RequestUserReport req) {
        Incidence incidence = new Incidence();
        incidence.setPublication(pub);
        Incidence savedIncidence = incidenceRepository.save(incidence);

        Report report = Report.builder()
                .incidence(savedIncidence)
                .reporter(reporter)
                .reason(req.getReason())
                .comment(req.getComment())
                .source(ReportSource.USER)
                .build();

        reportRepository.save(report);

        return ReportResponse.builder()
                .publicIncidenceUi(savedIncidence.getPublicUi())
                .publicationId(pub.getId())
                .publicationStatus(pub.getStatus())
                .message(Messages.REPORT_SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ReportResponse handleExistingIncidence(Incidence existing, Publication pub, User reporter, RequestUserReport req) {

        if (existing.getStatus().equals(IncidenceStatus.PENDING_REVIEW)) {
            throw new IncidenceAlreadyPendingReviewException(Messages.INCIDENCE_PENDING_REVIEW_CANNOT_ADD_REPORT);
        }

        if (existing.getStatus().equals(IncidenceStatus.UNDER_REVIEW)) {
            throw new PublicationUnderReviewException(Messages.INCIDENCE_UNDER_REVIEW_CANNOT_ADD_REPORT);
        }

        if (existing.getStatus().equals(IncidenceStatus.APPEALED)) {
            throw new IncidenceAppealedException(Messages.INCIDENCE_APPEALED_CANNOT_ADD_REPORT);
        }

        if (existing.getStatus().equals(IncidenceStatus.OPEN)) {
            Report report = Report.builder()
                    .incidence(existing)
                    .reporter(reporter)
                    .reason(req.getReason())
                    .comment(req.getComment())
                    .source(ReportSource.USER)
                    .build();

            existing.getReports().add(report);
            incidenceRepository.save(existing);
        }

        if (existing.getReports().size() >= REPORT_THRESHOLD) {
            pub.setUnderReview();
            existing.setStatus(IncidenceStatus.PENDING_REVIEW);
            publicationRepository.save(pub);
            incidenceRepository.save(existing);
            this.notifyUserOfPublicationBlock(existing);
        }

        return ReportResponse.builder()
                .publicIncidenceUi(existing.getPublicUi())
                .publicationId(pub.getId())
                .publicationStatus(pub.getStatus())
                .message(Messages.REPORT_SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    @Override
    public ReportResponse reportBySystem(RequestSystemReport req) {
        Long publicationId = req.getPublicationId();

        // Buscar incidencia activa y usuario del sistema
        Optional<Incidence> optionalIncidence = findActiveIncidence(publicationId);

        User systemUser = userRepository.findByUsername(SYSTEM_USERNAME)
                .orElseThrow(() -> new ReporterNotFoundException(Messages.USER_SYSTEM_NOT_FOUND));

        // ️ Si no existe incidencia previa, crear una nueva
        if (optionalIncidence.isEmpty()) {
            return createSystemIncidence(publicationId, systemUser, req);
        }

        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new PublicationNotFoundException(Messages.PUBLICATION_NOT_FOUND + publicationId));

        // Si ya existe, validar y agregar evidencia
        Incidence existingIncidence = optionalIncidence.get();

        if (existingIncidence.getStatus().equals(IncidenceStatus.APPEALED)) {
            throw new IncidenceAppealedException(Messages.INCIDENCE_APPEALED_CANNOT_ADD_REPORT);
        }

        Report report = Report.builder()
                .incidence(existingIncidence)
                .reporter(systemUser)
                .reason(req.getReason())
                .comment(req.getComment())
                .source(ReportSource.SYSTEM)
                .build();

        existingIncidence.getReports().add(report);

        if (existingIncidence.getStatus().equals(IncidenceStatus.OPEN)) {
            existingIncidence.setStatus(IncidenceStatus.PENDING_REVIEW);
            existingIncidence.getPublication().setUnderReview();
            publicationRepository.save(existingIncidence.getPublication());
        }

        incidenceRepository.save(existingIncidence);

        return ReportResponse.builder()
                .publicIncidenceUi(existingIncidence.getPublicUi())
                .publicationId(publicationId)
                .publicationStatus(publication.getStatus())
                .message(Messages.REPORT_SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Optional<Incidence> findActiveIncidence(Long publicationId) {
        List<IncidenceStatus> activeStatuses = List.of(
                IncidenceStatus.OPEN,
                IncidenceStatus.PENDING_REVIEW,
                IncidenceStatus.UNDER_REVIEW,
                IncidenceStatus.APPEALED
        );
        return incidenceRepository.findByPublicationIdAndStatusIn(publicationId, activeStatuses);
    }

    private ReportResponse createSystemIncidence(Long publicationId, User systemUser, RequestSystemReport req) {
        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new PublicationNotFoundException(Messages.PUBLICATION_NOT_FOUND + publicationId));

        publication.setUnderReview();

        Incidence newIncidence = new Incidence();
        newIncidence.setPublication(publication);
        newIncidence.setStatus(IncidenceStatus.PENDING_REVIEW);
        Incidence savedIncidence = incidenceRepository.save(newIncidence);

        Report systemReport = Report.builder()
                .incidence(savedIncidence)
                .reporter(systemUser)
                .reason(req.getReason())
                .comment(req.getComment())
                .source(ReportSource.SYSTEM)
                .build();

        reportRepository.save(systemReport);
        this.notifyUserOfPublicationBlock(savedIncidence);

        return ReportResponse.builder()
                .publicIncidenceUi(savedIncidence.getPublicUi())
                .publicationId(publicationId)
                .publicationStatus(publication.getStatus())
                .message(Messages.REPORT_SUCCESS)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<IncidenceSimpleDetailsResponse> fetchAllUnreviewed(Pageable pageable) {
        Page<Incidence> page = this.incidenceRepository.findAllUnreviewedWithDetails(pageable);
       return  generatePageIncidenceSimpleDetailResponse(page);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<IncidenceSimpleDetailsResponse> fetchAllReviewed(Pageable pageable) {
        Long currentUserId = securityService.getCurrentUserId();
        Page<Incidence> incidences = this.incidenceRepository.findAllReviewedWithDetails(currentUserId, pageable);
        return generatePageIncidenceSimpleDetailResponse(incidences);
    }

    // Esta incidencia se usa cuando se hace un fetch para las no revisadas y para las revisadas.
    // Si es revisada, tengo que verificar que el moderador sea el dueño de la incidencia.
    @Transactional(readOnly = true)
    @Override
    public IncidenceDetailsResponse fetchByPublicUiNativeProjection(UUID publicUi) {
        List<IncidenceDetailsProjection> projection = incidenceRepository.findByPublicUiWithDetailsNative(publicUi);

        if (projection.isEmpty()) {
            throw new IncidenceNotFoundException(Messages.INCIDENCE_NOT_FOUND + publicUi);
        }
        IncidenceDetailsProjection incidence = projection.getFirst();
        Long currentUserId = securityService.getCurrentUserId();

        if (incidence.getIncidenceModeratorId() != null && !incidence.getIncidenceModeratorId().equals(currentUserId)) {
            throw new AccessDeniedException(Messages.ACCESS_DENIED_TO_FIND_INCIDENCE);
        }

        return generateIncidenceDetailResponseUsingProjection(projection);
    }

    // Esto es parecido a la anterior, pero aqui tengo que verificar que el vendedor sea el dueño de la publicacion.
    @Override
    public IncidenceDetailsResponse fetchByPublicUiForSellerNativeProjection(UUID publicUi) {
        List<IncidenceDetailsProjection> projection = incidenceRepository.findByPublicUiWithDetailsNative(publicUi);

        if (projection.isEmpty()) {
            throw new IncidenceNotFoundException(Messages.INCIDENCE_NOT_FOUND + publicUi);
        }

        IncidenceDetailsProjection incidence = projection.getFirst();
        Long currentUserId = securityService.getCurrentUserId();

        if (!incidence.getPublicationVendorId().equals(currentUserId)) {
            throw new AccessDeniedException(Messages.ACCESS_DENIED_TO_FIND_INCIDENCE);
        }

        return generateIncidenceDetailResponseUsingProjection(projection);
    }

    public IncidenceDetailsResponse generateIncidenceDetailResponseUsingProjection(List<IncidenceDetailsProjection> projection) {
        IncidenceDetailsProjection incidence = projection.getFirst();

        // Ahora mapear los datos de la incidencia.
        IncidenceDetailsResponse response = new IncidenceDetailsResponse();
        response.setStatus(incidence.getIncidenceStatus());
        response.setAutoClosed(incidence.getIncidenceAutoclosed());
        response.setPublicIncidenceUi(incidence.getIncidencePublicUi());
        response.setIncidenceDecision(incidence.getIncidenceDecision());
        response.setModeratorComment(incidence.getIncidenceModeratorComment());
        response.setCreatedAt(incidence.getIncidenceCreatedAt());

        // Ahora a la publicacion
        SimplePublicationResponse publicationResponse = new SimplePublicationResponse();
        publicationResponse.setStatus(incidence.getPublicationStatus());
        publicationResponse.setId(incidence.getPublicationId());
        publicationResponse.setName(incidence.getPublicationName());
        publicationResponse.setDescription(incidence.getPublicationDescription());

        // Ahora cada reporte
        List<SimpleReportResponse> reportResponses = projection.stream().map( inc-> {

            SimpleReportResponse simpleResponse = new SimpleReportResponse();
            simpleResponse.setReason(inc.getReportReason());
            simpleResponse.setId(inc.getReportId());
            simpleResponse.setComment(inc.getReportComment());
            simpleResponse.setCreatedAt(inc.getReportCreatedAt());

            // El que reporta
            UserSimpleResponse reporter = new UserSimpleResponse();
            reporter.setEmail(inc.getReporterEmail());
            reporter.setFullname(inc.getReporterFullname());
            reporter.setId(inc.getReporterId());

            simpleResponse.setReporter(reporter);

            return simpleResponse;
        }).toList();
        response.setPublication(publicationResponse);
        response.setReports(reportResponses);

        return response;
    }

    // Generar una respuesta simple para una lista (paginacion)
    public Page<IncidenceSimpleDetailsResponse> generatePageIncidenceSimpleDetailResponse(Page<Incidence> page) {
        return page.map(i -> {
            IncidenceSimpleDetailsResponse detailsResponse = new IncidenceSimpleDetailsResponse();

            detailsResponse.setPublicIncidenceUi(i.getPublicUi());
            detailsResponse.setAutoClosed(i.getAutoclosed());
            detailsResponse.setCreatedAt(i.getCreatedAt());
            detailsResponse.setStatus(i.getStatus());
            detailsResponse.setIncidenceDecision(i.getDecision());

            // Publicacion
            SimplePublicationResponse publicationResponse = new SimplePublicationResponse();
            Publication pub = i.getPublication();
            publicationResponse.setId(pub.getId());
            publicationResponse.setDescription(pub.getDescription());
            publicationResponse.setStatus(pub.getStatus());
            publicationResponse.setName(pub.getName());
            detailsResponse.setPublication(publicationResponse);

            return detailsResponse;
        });
    }

    @Transactional
    @Override
    public ClaimIncidenceResponse claim(RequestClaimIncidence req) {
        Incidence incidence = incidenceRepository.findByPublicUi(req.getPublicIncidenceUi())
                .orElseThrow(() -> new IncidenceNotFoundException(Messages.INCIDENCE_NOT_FOUND + req.getPublicIncidenceUi()));

        // Esto es para verificar la parte del moderador bloqueado o inactivo.
        Long currentUserId = this.securityService.getCurrentUserId();
        User moderator = userRepository.findByIdNative(currentUserId)
                .orElseThrow(() -> new ModeratorNotFoundException(Messages.MODERATOR_NOT_FOUND + currentUserId));
        if ((moderator.getAccountStatus().equals(AccountStatus.INACTIVE) && moderator.getDeleted()) || moderator.getAccountStatus().equals(AccountStatus.BLOCKED)) {
            throw new AccessDeniedException(Messages.ACCESS_DENIED_TO_CLAIM_INCIDENCE);
        }

        // Esto es para verificar la parte del vendedor bloqueado o inactivo.
        UserIdProjection userIdProjection = incidenceRepository.findUserIdByPublicUi(req.getPublicIncidenceUi())
                .orElseThrow(() -> new IncidenceNotFoundException(Messages.USER_ID_PROJECTION_NOT_FOUND + req.getPublicIncidenceUi()));
        User user = userRepository.findByIdNative(userIdProjection.getUserId())
                .orElseThrow(() -> new UserNotFoundException(Messages.USER_NOT_FOUND + userIdProjection.getUserId()));
        if ((user.getAccountStatus().equals(AccountStatus.INACTIVE) && user.getDeleted()) || user.getAccountStatus().equals(AccountStatus.BLOCKED)) {
            throw new AccessDeniedException(Messages.ACCESS_DENIED_TO_CLAIM_INCIDENCE);
        }
        if (incidence.getStatus() != IncidenceStatus.OPEN && incidence.getStatus() != IncidenceStatus.PENDING_REVIEW) {
            throw new IncidenceNotClaimableException(Messages.INCIDENCE_NOT_CLAIMABLE + incidence.getStatus());
        }

        incidence.setStatus(IncidenceStatus.UNDER_REVIEW);
        incidence.setModerator(moderator);
        incidenceRepository.save(incidence);

        ClaimIncidenceResponse response = new ClaimIncidenceResponse();
        response.setPublicIncidenceUi(incidence.getPublicUi());
        response.setModeratorName(moderator.getFirstName() + " " + moderator.getLastName());
        response.setMessage(Messages.INCIDENCE_CLAIMED_SUCCESSFULLY);

        return response;
    }

    @Transactional
    @Override
    public DecisionResponse makeDecision(RequestMakeDecision req) {
        Incidence incidence = incidenceRepository.findByPublicUi(req.getPublicIncidenceUi())
                .orElseThrow(() -> new IncidenceNotFoundException(Messages.INCIDENCE_NOT_FOUND + req.getPublicIncidenceUi()));

        // Esto es para verificar la parte del moderador bloqueado o inactivo.
        Long currentUserId = this.securityService.getCurrentUserId();
        User moderator = userRepository.findByIdNative(currentUserId)
                .orElseThrow(() -> new ModeratorNotFoundException(Messages.MODERATOR_NOT_FOUND + currentUserId));
        if ((moderator.getAccountStatus().equals(AccountStatus.INACTIVE) && moderator.getDeleted()) || moderator.getAccountStatus().equals(AccountStatus.BLOCKED)) {
            throw new AccessDeniedException(Messages.ACCESS_DENIED_TO_CLAIM_INCIDENCE);
        }

        // Ver si el vendedor esta bloqeuado o deshabiltiado, si es asi el moderador/admin no puede tomar decision
        // ya que dana el flujo (ese vendedor no puede entrar al sistema, por lo tanto ya sea si acepta o apela la incidencia
        // no podra hacer nada).
        UserIdProjection userIdProjection = incidenceRepository.findUserIdByPublicUi(req.getPublicIncidenceUi())
                .orElseThrow(() -> new IncidenceNotFoundException(Messages.USER_ID_PROJECTION_NOT_FOUND + req.getPublicIncidenceUi()));
        User user = userRepository.findByIdNative(userIdProjection.getUserId())
                .orElseThrow(() -> new UserNotFoundException(Messages.USER_NOT_FOUND + userIdProjection.getUserId()));
        if ((user.getAccountStatus().equals(AccountStatus.INACTIVE) && user.getDeleted()) || user.getAccountStatus().equals(AccountStatus.BLOCKED)) {
            throw new AccessDeniedException(Messages.ACCESS_DENIED_TO_MAKE_DECISION_INCIDENCE);
        }

        if (!incidence.getStatus().equals(IncidenceStatus.UNDER_REVIEW)) {
            throw new IncidenceNotUnderReviewException(Messages.INCIDENCE_NOT_UNDER_REVIEW + incidence.getStatus());
        }

        // Buscar la incidencia y ver que el id del moderador concide con el currentUserId
        boolean belongsToModerator = incidenceRepository.existsByIdAndModeratorId(incidence.getId(), currentUserId);
        if (!belongsToModerator) {
            throw new IncidenceNotBelongToModeratorException(Messages.INCIDENCE_NOT_BELONG_TO_MODERATOR);
        }

        IncidenceDecision decision = req.getDecision();

        incidence.setModeratorComment(req.getComment());
        incidence.setDecision(decision);
        incidence.setStatus(IncidenceStatus.RESOLVED);

        if (decision.equals(IncidenceDecision.APPROVED)) {
            incidence.getPublication().setVisible();

            // En ecendia, aqui siempre la incidencia estaria open, si no entra. Si entra, signficia que esta bajo revision.
            if (incidence.getReports().size() >= REPORT_THRESHOLD) {
                this.notifyUserOfPublicationUnlock(incidence);
            }
        }

        if (decision.equals(IncidenceDecision.REJECTED)) {
            incidence.getPublication().setBlocked();
            this.notifyUserOfAppealAvailability(incidence);
        }

        incidenceRepository.save(incidence);

        return new DecisionResponse(
                incidence.getPublicUi(),
                incidence.getDecision(),
                incidence.getStatus().name(),
                Messages.DECISION_PROCESSED_SUCCESSFULLY
        );
    }

    @Transactional
    @Override
    public AppealResponse   appeal(RequestAppealIncidence req) {

        VendorIdProjection projection = incidenceRepository.findVendorIdByIncidencePublicUi(req.getPublicIncidenceUi())
                .orElseThrow(() -> new IncidenceNotFoundException(Messages.VENDOR_ID_PROJECTION_NOT_FOUND + req.getPublicIncidenceUi()));

        // Primero voy a checar si el vendedor esta habilitado para apelar.
        boolean isSellerActive = userRepository.existsByIdNative(projection.getVendorId());
        if (!isSellerActive) {
            throw new IncidenceAppealNotAllowedException(Messages.SELLER_NOT_ACTIVE_CANNOT_APPEAL);
        }

        Incidence incidence = incidenceRepository.findByPublicUiEager(req.getPublicIncidenceUi())
                .orElseThrow(() -> new IncidenceNotFoundException(Messages.INCIDENCE_NOT_FOUND + req.getPublicIncidenceUi()));

        // verificar que este rechazada
        if (!incidence.getDecision().equals(IncidenceDecision.REJECTED)) {
            throw new IncidenceNotAppealableException(Messages.INCIDENCE_NOT_APPEALABLE);
        }

        // ver si esa incidencia ya tiene una apelacion, si es asi, no puede apelar de nuevo.
        boolean hasAppeal = appealRepository.existsByIncidenceId(incidence.getId());
        if (hasAppeal) {
            throw new IncidenceAlreadyAppealedException(Messages.INCIDENCE_ALREADY_APPEALED);
        }

        // el que hace la apelacion (usuario actual)
        Long currentUserId = this.securityService.getCurrentUserId();
        User seller = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ReporterNotFoundException(Messages.REPORTER_NOT_FOUND + currentUserId));

        // chequear qeu el usuario actual, sea realmente el vendedor de la publicacion asociada a la incidencia
        if (!incidence.getPublication().getVendor().getId().equals(currentUserId)) {
            throw new IncidenceAppealNotAllowedException(Messages.SELLER_NOT_OWNER_OF_PUBLICATION);
        }

        incidence.setStatus(IncidenceStatus.APPEALED);
        Incidence appealedIncidence = incidenceRepository.save(incidence);

        Appeal appeal = new Appeal();
        appeal.setIncidence(appealedIncidence);
        appeal.setReason(req.getReason());
        appeal.setSeller(seller);

        // Calculo del nuevo moderador.
        User previousModerator = appealedIncidence.getModerator();
        ModeratorInfo previousModeratorInfo = new ModeratorInfo();
        previousModeratorInfo.setId(previousModerator.getId());
        previousModeratorInfo.setFullname(previousModerator.getFullName());
        previousModeratorInfo.setEmail(previousModerator.getEmail());

        Long newModeratorId = userRepository.findLeastBusyModeratorOrAdminExcludingId(previousModerator.getId());

        if (newModeratorId == null) {
            // No hay otros moderadores disponibles. Entonces pasa a una cola de espera, pero el cliente sabra que cada dia a las 3,30 am
            // se intentara asignar automaticamente otro moderador.
            appeal.setStatus(AppealStatus.FAILED_NO_MOD);
            Appeal saved = appealRepository.save(appeal);
            this.sendAppealPendingAssignmentEmailToSeller(saved);

            return AppealResponse.builder()
                    .appealId(saved.getId())
                    .publicIncidenceUi(appealedIncidence.getPublicUi())
                    .message(Messages.APPEAL_CREATED_PENDING_MODERATOR)
                    .status(AppealStatus.FAILED_NO_MOD)
                    .createdAt(LocalDateTime.now())
                    .previousModerator(previousModeratorInfo)
                    .newModerator(null)
                    .build();
        }

        // Si hay moderadores disponibles
        User newModerator = userRepository.findById(newModeratorId)
                .orElseThrow(() -> new ModeratorNotFoundException(Messages.MODERATOR_NOT_FOUND + newModeratorId));

        appeal.setNewModerator(newModerator);
        appeal.setStatus(AppealStatus.ASSIGNED);
        Appeal saved = appealRepository.save(appeal);

        ModeratorInfo newModeratorInfo = new ModeratorInfo();
        newModeratorInfo.setId(newModerator.getId());
        newModeratorInfo.setFullname(newModerator.getFullName());
        newModeratorInfo.setEmail(newModerator.getEmail());

        this.notifyUserOfAppealSubmission(incidence);
        this.notifyModeratorOfNewAppeal(incidence, newModeratorInfo, saved);

        // formar respuesta final
        return AppealResponse.builder()
                .appealId(saved.getId())
                .publicIncidenceUi(appealedIncidence.getPublicUi())
                .message(Messages.APPEAL_CREATED_SUCCESS)
                .status(AppealStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .previousModerator(previousModeratorInfo)
                .newModerator(newModeratorInfo)
                .build();
    }

    private void notifyUserOfPublicationBlock(Incidence incidence) {
        Publication pub = incidence.getPublication();
        User vendor = pub.getVendor();

        Map<String, String> variables = Map.of(
                "fullName", vendor.getFullName(),
                "publicationName", pub.getName()
        );
        notificationService.sendAsync(vendor.getEmail(), EmailType.PUBLICATION_BLOCKED_NOTIFICATION, variables);
    }

    public void notifyUserOfPublicationUnlock(Incidence incidence) {
        Publication pub = incidence.getPublication();
        User vendor = pub.getVendor();

        Map<String, String> variables = Map.of(
                "fullName", vendor.getFullName(),
                "publicationName", pub.getName()
        );
        notificationService.sendAsync(vendor.getEmail(), EmailType.PUBLICATION_UNLOCK_NOTIFICATION, variables);
    }

    public void notifyUserOfAppealAvailability(Incidence incidence) {
        Publication pub = incidence.getPublication();
        User vendor = pub.getVendor();

        String uriFrontend = frontendUrl + "/marketplace-refactored" + "/incidencias/" + incidence.getPublicUi() + "/apelacion";

        Map<String, String> variables = Map.of(
                "fullName", vendor.getFullName(),
                "publicationName", pub.getName(),
                "appealLink", uriFrontend
        );
        notificationService.sendAsync(vendor.getEmail(), EmailType.PUBLICATION_APPEAL_AVAILABLE_NOTIFICATION, variables);
    }

    public void sendAppealPendingAssignmentEmailToSeller(Appeal appeal) {
        Publication pub = appeal.getIncidence().getPublication();
        User vendor = appeal.getSeller();

        Map<String, String> variables = Map.of(
                "fullName", vendor.getFullName(),
                "publicationName", pub.getName()
        );

        notificationService.sendAsync(
                vendor.getEmail(),
                EmailType.APPEAL_PENDING_ASSIGNMENT_NOTIFICATION,
                variables
        );
    }

    public void notifyUserOfAppealSubmission(Incidence incidence) {
        Publication pub = incidence.getPublication();
        User vendor = pub.getVendor();

        Map<String, String> variables = Map.of(
                "fullName", vendor.getFullName(),
                "publicationName", pub.getName()
        );
        notificationService.sendAsync(vendor.getEmail(), EmailType.APPEAL_RECEIVED_CONFIRMATION_NOTIFICATION, variables);
    }

    public void notifyModeratorOfNewAppeal(Incidence incidence, ModeratorInfo moderatorInfo, Appeal appeal) {
        Publication pub = incidence.getPublication();
        User vendor = pub.getVendor();
        String newEmail = moderatorInfo.getEmail();

        String uriFrontend = frontendUrl + "/marketplace-refactored" + "/apelaciones/" + appeal.getId();

        Map<String, String> variables = Map.of(
                "moderatorName", moderatorInfo.getFullname(),
                "fullName", vendor.getFullName(),
                "publicationName", pub.getName(),
                "moderationPanelLink", uriFrontend
        );
        notificationService.sendAsync(newEmail, EmailType.APPEAL_ASSIGNED_TO_MODERATOR_NOTIFICATION, variables);
    }

}
