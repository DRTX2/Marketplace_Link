package com.gpis.marketplace_link.services.incidence;

import com.gpis.marketplace_link.dto.Messages;
import com.gpis.marketplace_link.dto.incidence.AppealResponse;
import com.gpis.marketplace_link.dto.incidence.*;
import com.gpis.marketplace_link.entities.*;
import com.gpis.marketplace_link.enums.*;
import com.gpis.marketplace_link.exceptions.business.incidences.IncidenceNotAppealableException;
import com.gpis.marketplace_link.exceptions.business.incidences.*;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationNotFoundException;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationUnderReviewException;
import com.gpis.marketplace_link.exceptions.business.users.ModeratorNotFoundException;
import com.gpis.marketplace_link.exceptions.business.users.ReporterNotFoundException;
import com.gpis.marketplace_link.exceptions.business.users.UserBlockedException;
import com.gpis.marketplace_link.repositories.*;
import com.gpis.marketplace_link.security.service.SecurityService;
import com.gpis.marketplace_link.services.NotificationService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IncidenceServiceImp implements IncidenceService {

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    private final NotificationService notificationService;
    private final SecurityService securityService;

    private final IncidenceRepository incidenceRepository;
    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final AppealRepository appealRepository;
    private final UserBlockLogRepository userBlockLogRepository;

    private static final int REPORT_THRESHOLD = 10;
    private static final int REPORT_WINDOW_HOURS = 1;
    private static final int MAX_REPORTS_PER_HOUR = 3;
    private static final int BLOCK_DURATION_HOURS = 6;

    private static final String SYSTEM_USERNAME = "system_user";

    @Transactional
    @Override
    public void autoclose() {
        int hours = 24;
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        incidenceRepository.bulkAutoClose(cutoff);
    }

    @Transactional(noRollbackFor = { UserBlockedException.class })
    @Override
    public ReportResponse reportByUser(RequestUserReport req) {
        List<IncidenceStatus> status = List.of(
                IncidenceStatus.OPEN,
                IncidenceStatus.UNDER_REVIEW,
                IncidenceStatus.APPEALED);

        Long reporterId = securityService.getCurrentUserId();
        Long publicationId = req.getPublicationId();

        // Validar bloqueos existentes
        this.validateActiveBlock(reporterId, publicationId);

        // Obtener entidades base
        Publication pub = publicationRepository.findById(publicationId).orElseThrow(() -> new PublicationNotFoundException(Messages.PUBLICATION_NOT_FOUND + publicationId));
        User reporter = userRepository.findById(reporterId).orElseThrow(() -> new ReporterNotFoundException(Messages.REPORTER_NOT_FOUND + reporterId));

        // Validar si el usuario intenta reportar su propia publicación
        this.validateNotSelfReporting(pub, reporter);

        // Verificar cantidad de reportes recientes y bloquear si excede
        this.validateReportFrequency(reporter, pub);

        // Crear o actualizar incidencia
        Optional<Incidence> inc = incidenceRepository.findByPublicationIdAndStatusIn(publicationId, status);
        if (inc.isEmpty()) {
            return handleNewIncidence(pub, reporter, req);
        } else {
            return handleExistingIncidence(inc.get(), pub, reporter, req);
        }
    }

    private void validateActiveBlock(Long reporterId, Long publicationId) {
        Optional<UserBlockLog> activeBlock = userBlockLogRepository
                .findActiveBlockForReport(reporterId, publicationId, LocalDateTime.now());

        if (activeBlock.isPresent()) {
            throw new ReporterBlockedFromReportingException(Messages.REPORTER_BLOCKED_FROM_REPORTING + activeBlock.get().getBlockedUntil()
            );
        }
    }

    private void validateNotSelfReporting(Publication pub, User reporter) {
        if (pub.getVendor().getId().equals(reporter.getId())) {
            throw new IncidenceNotAllowedToReportOwnPublicationException("No puedes reportar tu propia publicación.");
        }
    }

    private void validateReportFrequency(User reporter, Publication pub) {
        int reportsInLastHour = reportRepository.countByReporterIdAndPublicationIdAndCreatedAtAfter(
                reporter.getId(),
                pub.getId(),
                LocalDateTime.now().minusHours(REPORT_WINDOW_HOURS)
        );

        if (reportsInLastHour >= MAX_REPORTS_PER_HOUR) {
            UserBlockLog block = new UserBlockLog();
            block.setUser(reporter);
            block.setTargetPublication(pub);
            block.setReason(Messages.BLOCK_REASON_SPAM_REPORTS);
            block.setBlockedAction(Messages.BLOCK_ACTION_REPORT);
            block.setBlockedUntil(LocalDateTime.now().plusHours(BLOCK_DURATION_HOURS));
            block.setCreatedAt(LocalDateTime.now());
            userBlockLogRepository.save(block);

            throw new UserBlockedException(Messages.USER_BLOCKED_FOR_SPAM_REPORTS + block.getBlockedUntil());
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
        return buildReportResponse(savedIncidence.getId(), pub.getId(), Messages.REPORT_SUCCESS);
    }

    private ReportResponse handleExistingIncidence(Incidence existing, Publication pub, User reporter, RequestUserReport req) {
        if (existing.getStatus().equals(IncidenceStatus.UNDER_REVIEW)) {
            throw new PublicationUnderReviewException(Messages.PUBLICATION_UNDER_REVIEW_CANNOT_ADD_REPORT);
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
            existing.setStatus(IncidenceStatus.UNDER_REVIEW);
            publicationRepository.save(pub);
            incidenceRepository.save(existing);
            this.notifyUserOfBlockAndAppealOption(existing);
        }

        return buildReportResponse(existing.getId(), pub.getId(), Messages.REPORT_SUCCESS);
    }

    @Transactional
    @Override
    public ReportResponse reportBySystem(RequestSystemReport req) {
        Long publicationId = req.getPublicationId();

        // Buscar incidencia activa y usuario del sistema
        Optional<Incidence> optionalIncidence = findActiveIncidence(publicationId);
        User systemUser = findSystemUser();

        // ️ Si no existe incidencia previa, crear una nueva
        if (optionalIncidence.isEmpty()) {
            return createSystemIncidence(publicationId, systemUser, req);
        }

        // Si ya existe, validar y agregar evidencia
        Incidence existingIncidence = optionalIncidence.get();
        this.validateNotAppealed(existingIncidence);
        this.addSystemReport(existingIncidence, systemUser, req);
        this.updateStatusIfOpen(existingIncidence);
        incidenceRepository.save(existingIncidence);

        return buildReportResponse(existingIncidence.getId(), publicationId, Messages.REPORT_SUCCESS);
    }

    private Optional<Incidence> findActiveIncidence(Long publicationId) {
        List<IncidenceStatus> activeStatuses = List.of(
                IncidenceStatus.OPEN,
                IncidenceStatus.UNDER_REVIEW,
                IncidenceStatus.APPEALED
        );
        return incidenceRepository.findByPublicationIdAndStatusIn(publicationId, activeStatuses);
    }

    private User findSystemUser() {
        return userRepository.findByUsername(SYSTEM_USERNAME)
                .orElseThrow(() -> new ReporterNotFoundException(Messages.USER_SYSTEM_NOT_FOUND));
    }

    private ReportResponse createSystemIncidence(Long publicationId, User systemUser, RequestSystemReport req) {
        Publication publication = publicationRepository.findById(publicationId)
                .orElseThrow(() -> new PublicationNotFoundException(Messages.PUBLICATION_NOT_FOUND + publicationId));

        publication.setUnderReview();

        Incidence newIncidence = new Incidence();
        newIncidence.setPublication(publication);
        newIncidence.setStatus(IncidenceStatus.UNDER_REVIEW);
        Incidence savedIncidence = incidenceRepository.save(newIncidence);

        Report systemReport = Report.builder()
                .incidence(savedIncidence)
                .reporter(systemUser)
                .reason(req.getReason())
                .comment(req.getComment())
                .source(ReportSource.SYSTEM)
                .build();

        reportRepository.save(systemReport);
        this.notifyUserOfBlockAndAppealOption(savedIncidence);

        return buildReportResponse(savedIncidence.getId(), publicationId, Messages.REPORT_SUCCESS);
    }

    private void notifyUserOfBlockAndAppealOption(Incidence incidence) {
        Publication pub = incidence.getPublication();
        User vendor = pub.getVendor();

        Map<String, String> variables = Map.of(
                "fullName", vendor.getFullName(),
                "publicationName", pub.getName(),
                "appealLink", this.frontendUrl + "/appeals/" + incidence.getPublicId()
        );
        notificationService.sendAsync(vendor.getEmail(), EmailType.PUBLICATION_BLOCKED_NOTIFICATION, variables);
    }

    private void validateNotAppealed(Incidence incidence) {
        if (incidence.getStatus().equals(IncidenceStatus.APPEALED)) {
            throw new IncidenceAppealedException(Messages.INCIDENCE_APPEALED_CANNOT_ADD_REPORT);
        }
    }

    private void addSystemReport(Incidence incidence, User systemUser, RequestSystemReport req) {
        Report report = Report.builder()
                .incidence(incidence)
                .reporter(systemUser)
                .reason(req.getReason())
                .comment(req.getComment())
                .source(ReportSource.SYSTEM)
                .build();

        incidence.getReports().add(report);
    }

    private void updateStatusIfOpen(Incidence incidence) {
        if (incidence.getStatus().equals(IncidenceStatus.OPEN)) {
            incidence.setStatus(IncidenceStatus.UNDER_REVIEW);
            incidence.getPublication().setUnderReview();
            publicationRepository.save(incidence.getPublication());
        }
    }

    private ReportResponse buildReportResponse(Long incidenceId, Long publicationId, String message) {
        return ReportResponse.builder()
                .incidenceId(incidenceId)
                .productId(publicationId)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<IncidenceDetailsResponse> fetchAllUnreviewed(Pageable pageable) {
        Page<Incidence> page = this.incidenceRepository.findAllUnreviewedWithDetails(pageable);
       return  generatePageIncidenceDetailResponse(page);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<IncidenceDetailsResponse> fetchAllReviewed(Pageable pageable) {
        Long currentUserId = securityService.getCurrentUserId();
        Page<Incidence> incidences = this.incidenceRepository.findAllReviewedWithDetails(currentUserId, pageable);
        return generatePageIncidenceDetailResponse(incidences);
    }

    public List<IncidenceDetailsResponse> generateIncidenceDetailResponse(@NotNull List<Incidence> incidences) {
        return incidences.stream().map(i -> {

            IncidenceDetailsResponse detailsResponse = new IncidenceDetailsResponse();

            detailsResponse.setId(i.getId());
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

            // Reportes
            List<SimpleReportResponse> reports = i.getReports().stream().map(r -> {

                SimpleReportResponse simpleResponse = new SimpleReportResponse();
                User reporter = r.getReporter();

                UserSimpleResponse userSimpleResponse = new UserSimpleResponse();
                userSimpleResponse.setId(reporter.getId());
                userSimpleResponse.setEmail(reporter.getEmail());
                userSimpleResponse.setFullname(reporter.getFullName());

                simpleResponse.setId(r.getId());
                simpleResponse.setComment(r.getComment());
                simpleResponse.setReason(r.getReason());
                simpleResponse.setReporter(userSimpleResponse);

                return simpleResponse;
            }).toList();
            detailsResponse.setReports(reports);

            return detailsResponse;
        }).toList();
    }

    public Page<IncidenceDetailsResponse> generatePageIncidenceDetailResponse(Page<Incidence> page) {
        return page.map(i -> {

            IncidenceDetailsResponse detailsResponse = new IncidenceDetailsResponse();

            detailsResponse.setId(i.getId());
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

            // Reportes
            List<SimpleReportResponse> reports = i.getReports().stream().map(r -> {

                SimpleReportResponse simpleResponse = new SimpleReportResponse();
                User reporter = r.getReporter();

                UserSimpleResponse userSimpleResponse = new UserSimpleResponse();
                userSimpleResponse.setId(reporter.getId());
                userSimpleResponse.setEmail(reporter.getEmail());
                userSimpleResponse.setFullname(reporter.getFullName());

                simpleResponse.setId(r.getId());
                simpleResponse.setComment(r.getComment());
                simpleResponse.setReason(r.getReason());
                simpleResponse.setReporter(userSimpleResponse);

                return simpleResponse;
            }).toList();
            detailsResponse.setReports(reports);

            return detailsResponse;
        });
    }

    @Transactional
    @Override
    public ClaimIncidenceResponse claim(RequestClaimIncidence req) {

        Incidence incidence = incidenceRepository.findByPublicId(req.getPublicIncidenceUi())
                .orElseThrow(() -> new IncidenceNotFoundException(Messages.INCIDENCE_NOT_FOUND + req.getPublicIncidenceUi()));

        Long currentUserId = this.securityService.getCurrentUserId();

        User moderator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ModeratorNotFoundException(Messages.MODERATOR_NOT_FOUND + currentUserId));

        if (incidence.getModerator() != null) {
            throw new IncidenceAlreadyClaimedException(Messages.INCIDENCE_ALREADY_CLAIMED);
        }
        if (incidence.getDecision() != null) {
            throw new IncidenceAlreadyDecidedException(Messages.INCIDENCE_ALREADY_DECIDED);
        }
        if (Boolean.TRUE.equals(incidence.getAutoclosed())) {
            throw new IncidenceAlreadyClosedException(Messages.INCIDENCE_ALREADY_CLOSED);
        }

        if (incidence.getStatus() == IncidenceStatus.OPEN) {
            incidence.setStatus(IncidenceStatus.UNDER_REVIEW);
        } else if (incidence.getStatus() != IncidenceStatus.UNDER_REVIEW) {
            // si no esta ni OPEN ni UNDER_REVIEW, no se puede reclamar
            throw new IncidenceNotClaimableException(Messages.INCIDENCE_NOT_CLAIMABLE + incidence.getStatus());
        }

        incidence.setModerator(moderator);
        incidenceRepository.save(incidence);

        ClaimIncidenceResponse response = new ClaimIncidenceResponse();
        response.setIncidenceId(incidence.getId());
        response.setModeratorName(moderator.getFirstName() + " " + moderator.getLastName());
        response.setMessage(Messages.INCIDENCE_CLAIMED_SUCCESSFULLY);

        return response;
    }

    @Transactional
    @Override
    public DecisionResponse makeDecision(RequestMakeDecision req) {

        Incidence incidence = incidenceRepository.findByPublicId(req.getPublicIncidenceUi())
                .orElseThrow(() -> new IncidenceNotFoundException(Messages.INCIDENCE_NOT_FOUND + req.getPublicIncidenceUi()));

        if (!incidence.getStatus().equals(IncidenceStatus.UNDER_REVIEW)) {
            throw new IncidenceNotUnderReviewException(Messages.INCIDENCE_NOT_UNDER_REVIEW + incidence.getStatus());
        }

        // Solo puedo hacer la deceision si la incidencia es mia (como moderador)
        Long currentUserId = this.securityService.getCurrentUserId();

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
        }

        if (decision.equals(IncidenceDecision.REJECTED)) {
            incidence.getPublication().setBlocked();
        }

        incidenceRepository.save(incidence);

        return new DecisionResponse(
                incidence.getId(),
                incidence.getDecision(),
                incidence.getStatus().name(),
                Messages.DECISION_PROCESSED_SUCCESSFULLY
        );
    }

    @Transactional
    @Override
    public AppealResponse appeal(RequestAppealIncidence req) {

        // verificar existencia
        Incidence incidence = incidenceRepository.findByPublicId(req.getPublicIncidenceUi())
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

        incidence.setStatus(IncidenceStatus.APPEALED);
        Incidence appealedIncidence = incidenceRepository.save(incidence);

        User seller = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ReporterNotFoundException(Messages.REPORTER_NOT_FOUND + currentUserId));

        Appeal appeal = new Appeal();
        appeal.setIncidence(appealedIncidence);
        appeal.setReason(req.getReason());
        appeal.setSeller(seller);

        // Calculo del nuevo moderador.
        User previousModerator = appealedIncidence.getModerator();
        Long newModeratorId = userRepository.findLeastBusyModeratorExcludingId(previousModerator.getId());

        if (newModeratorId == null) {
            // No hay otros moderadores disponibles. Entonces pasa a una cola de espera, pero el cliente sabra que cada dia a las 3,30 am
            // se intentara asignar automaticamente otro moderador.
            appeal.setStatus(AppealStatus.FAILED_NO_MOD);
            Appeal saved = appealRepository.save(appeal);

            return AppealResponse.builder()
                    .appealId(saved.getId())
                    .incidenceId(appealedIncidence.getId())
                    .message(Messages.APPEAL_CREATED_PENDING_MODERATOR)
                    .status(AppealStatus.FAILED_NO_MOD)
                    .createdAt(LocalDateTime.now())
                    .previousModerator(null)
                    .newModerator(null)
                    .build();
        }

        // Si hay moderadores disponibles
        User newModerator = userRepository.findById(newModeratorId)
                .orElseThrow(() -> new ModeratorNotFoundException(Messages.MODERATOR_NOT_FOUND + newModeratorId));

        appeal.setNewModerator(newModerator);
        appeal.setStatus(AppealStatus.ASSIGNED);
        Appeal saved = appealRepository.save(appeal);

        // formar los dtos para moderadores
        ModeratorInfo previousModeratorInfo = new ModeratorInfo();
        previousModeratorInfo.setId(previousModerator.getId());
        previousModeratorInfo.setFullName(previousModerator.getFullName());
        previousModeratorInfo.setEmail(previousModerator.getEmail());

        ModeratorInfo newModeratorInfo = new ModeratorInfo();
        newModeratorInfo.setId(newModerator.getId());
        newModeratorInfo.setFullName(newModerator.getFullName());
        newModeratorInfo.setEmail(newModerator.getEmail());

        // formar respuesta final
        return AppealResponse.builder()
                .appealId(saved.getId())
                .incidenceId(appealedIncidence.getId())
                .message(Messages.APPEAL_CREATED_SUCCESS)
                .status(AppealStatus.ASSIGNED)
                .createdAt(LocalDateTime.now())
                .previousModerator(previousModeratorInfo)
                .newModerator(newModeratorInfo)
                .build();
    }


}
