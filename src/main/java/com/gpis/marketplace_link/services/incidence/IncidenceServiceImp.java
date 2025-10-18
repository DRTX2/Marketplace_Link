package com.gpis.marketplace_link.services.incidence;

import com.gpis.marketplace_link.dto.Messages;
import com.gpis.marketplace_link.dto.incidence.*;
import com.gpis.marketplace_link.entities.Incidence;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.entities.Report;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import com.gpis.marketplace_link.enums.ReportSource;
import com.gpis.marketplace_link.exceptions.business.incidences.*;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationNotFoundException;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationUnderReviewException;
import com.gpis.marketplace_link.exceptions.business.users.ModeratorNotFoundException;
import com.gpis.marketplace_link.exceptions.business.users.ReporterNotFoundException;
import com.gpis.marketplace_link.repositories.IncidenceRepository;
import com.gpis.marketplace_link.repositories.PublicationRepository;
import com.gpis.marketplace_link.repositories.ReportRepository;
import com.gpis.marketplace_link.repositories.UserRepository;
import com.gpis.marketplace_link.security.service.SecurityService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidenceServiceImp implements IncidenceService {

    private final SecurityService securityService;
    private final IncidenceRepository incidenceRepository;
    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private static final int REPORT_THRESHOLD = 3;
    private static final String SYSTEM_USERNAME = "system_user";

    @Transactional
    @Override
    public void autoclose() {
        int hours = 24;
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        incidenceRepository.bulkAutoClose(cutoff);
    }

    @Transactional
    @Override
    public ReportResponse reportByUser(RequestUserReport req) {
        // Datos para o crear la incidencia o agregar el reporte a la incidencia existente.
        Long publicationId = req.getPublicationId();
        List<IncidenceStatus> status = List.of(IncidenceStatus.OPEN, IncidenceStatus.UNDER_REVIEW, IncidenceStatus.APPEALED);
        Optional<Incidence> inc = incidenceRepository.findByPublicationIdAndStatusIn(publicationId, status);

        // No existe incidencia para ese producto, entonces crear la incidencia y el reporte.
        if (inc.isEmpty()) {
            Publication savedPublication = publicationRepository.findById(publicationId).orElseThrow(() -> new PublicationNotFoundException(Messages.PUBLICATION_NOT_FOUND + publicationId));

            Incidence incidence = new Incidence();
            incidence.setPublication(savedPublication);
            Incidence savedIncidence = incidenceRepository.save(incidence);

            User reporter = userRepository.findById(req.getReporterId()).orElseThrow(() -> new ReporterNotFoundException(Messages.REPORTER_NOT_FOUND + req.getReporterId()));

            Report report =
                    Report.builder()
                            .incidence(incidence)
                            .reporter(reporter)
                            .reason(req.getReason())
                            .comment(req.getComment())
                            .source(ReportSource.USER)
                            .build();

            reportRepository.save(report);

            return buildReportResponse(savedIncidence.getId(), publicationId, Messages.REPORT_AUTO);

        } else {
            // Como existe la incidencia se considera casos como
            Incidence existingIncidence = inc.get();

            // El producto esta en revision, no se pueden agregar mas reportes.
            if (existingIncidence.getStatus().equals(IncidenceStatus.UNDER_REVIEW)) {
                throw new PublicationUnderReviewException(Messages.PUBLICATION_UNDER_REVIEW_CANNOT_ADD_REPORT);
            }

            // La incidencia esta apelada, no se pueden agregar mas reportes.
            if (existingIncidence.getStatus().equals(IncidenceStatus.APPEALED)) {
                throw new IncidenceAppealedException(Messages.INCIDENCE_APPEALED_CANNOT_ADD_REPORT);
            }

            // La incidencia esta abierta, se puede agregar el reporte.
            if (existingIncidence.getStatus().equals(IncidenceStatus.OPEN)) {
                User reporter = userRepository
                                        .findById(req.getReporterId())
                                        .orElseThrow(() -> new ReporterNotFoundException(Messages.REPORTER_NOT_FOUND + req.getReporterId()));

                Report report =
                        Report.builder()
                                        .incidence(existingIncidence)
                                        .reporter(reporter)
                                        .reason(req.getReason())
                                        .comment(req.getComment())
                                        .source(ReportSource.USER)
                                        .build();

                existingIncidence.getReports().add(report);
                existingIncidence.setLastReportAt(LocalDateTime.now());
                incidenceRepository.save(existingIncidence);
            }

            // Si la cantidad de reportes es mayor o igual a 3, se cambia el estado de la publicacion bajo revision.
            if (existingIncidence.getReports().size() >= REPORT_THRESHOLD) {
                Publication savedPublication = publicationRepository
                                                        .findById(publicationId)
                                                        .orElseThrow(() -> new PublicationNotFoundException(Messages.PUBLICATION_NOT_FOUND + publicationId));
                savedPublication.setUnderReview();
                existingIncidence.setStatus(IncidenceStatus.UNDER_REVIEW);
                publicationRepository.save(savedPublication);
                incidenceRepository.save(existingIncidence);
            }

            return buildReportResponse(existingIncidence.getId(), publicationId, Messages.REPORT_SUCCESS);
        }
    }

    @Transactional
    @Override
    public ReportResponse reportBySystem(RequestSystemReport req) {
        Long publicationId = req.getPublicationId();
        List<IncidenceStatus> status = List.of(
                IncidenceStatus.OPEN,
                IncidenceStatus.UNDER_REVIEW,
                IncidenceStatus.APPEALED
        );

        Optional<Incidence> optional = incidenceRepository.findByPublicationIdAndStatusIn(publicationId, status);
        User systemUser = userRepository.findByUsername(SYSTEM_USERNAME).orElseThrow(() -> new ReporterNotFoundException(Messages.USER_SYSTEM_NOT_FOUND));

        if (optional.isEmpty()) {
            Incidence incidence = new Incidence();

            Publication savedPublication = publicationRepository
                    .findById(publicationId)
                    .orElseThrow(() -> new PublicationNotFoundException(Messages.PUBLICATION_NOT_FOUND + publicationId));
            savedPublication.setUnderReview();

            incidence.setPublication(savedPublication);
            incidence.setStatus(IncidenceStatus.UNDER_REVIEW);
            Incidence savedIncidence = incidenceRepository.save(incidence);

            Report report = Report.builder()
                            .incidence(incidence)
                            .reporter(systemUser)
                            .reason(req.getReason())
                            .comment((req.getComment()))
                            .source(ReportSource.SYSTEM)
                            .build();

            report.setSource(ReportSource.SYSTEM);
            reportRepository.save(report);

            return buildReportResponse(savedIncidence.getId(), publicationId, Messages.REPORT_SUCCESS);
        }

        Incidence inc = optional.get();

        // El caso ya no esta abierto a nueva evidencia. Se evalua si la decision tomada fue correcta en base a lo que ya existia antes de la apelacion.
        // Por eso, si esta apelada no se puede agregar mas evidencia.
        if (inc.getStatus().equals(IncidenceStatus.APPEALED)) {
            throw new IncidenceAppealedException(Messages.INCIDENCE_APPEALED_CANNOT_ADD_REPORT);
        }

        // Si la incidencia esta bajo revision, se agrega neuva evidencia. Eso se diferencia de un usuario
        // que si esta bajo revision, no puede agregar mas (porque puede ser informacion "falsa" sabiendo que su publicacion esta bajo revision).
        Report report = Report.builder()
                        .incidence(inc)
                        .reporter(systemUser)
                        .reason(req.getReason())
                        .comment(req.getComment())
                        .source(ReportSource.SYSTEM).build();

        log.info("Adding system report to incidence id={} for publication id={}", report.getSource(), report.getId());
        inc.getReports().add(report);

        // Ahora, si esa incidencai esta abierta, automaticamente pasa a estar bajo revision.
        if (inc.getStatus().equals(IncidenceStatus.OPEN)) {
            inc.setStatus(IncidenceStatus.UNDER_REVIEW);
            inc.getPublication().setUnderReview();
            publicationRepository.save(inc.getPublication());
        }

        incidenceRepository.save(inc);
        return buildReportResponse(inc.getId(), publicationId, Messages.REPORT_SUCCESS);
    }

    private ReportResponse buildReportResponse(Long incidenceId, Long publicationId, String message) {
        return ReportResponse.builder()
                .incidenceId(incidenceId)
                .productId(publicationId)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Override
    public List<IncidenceDetailsResponse> fetchAllUnreviewed() {
        List<Incidence> incidences = this.incidenceRepository.findAllUnreviewedWithDetails();
       return generateIncidenceDetailResponse(incidences);
    }

    @Override
    public List<IncidenceDetailsResponse> fetchAllReviewed() {
        Long currentUserId = securityService.getCurrentUserId();
        List<Incidence> incidences = this.incidenceRepository.findAllReviewedWithDetails(currentUserId);
        return generateIncidenceDetailResponse(incidences);
    }

    public List<IncidenceDetailsResponse> generateIncidenceDetailResponse(@NotNull List<Incidence> incidences) {
        return incidences.stream().map(i -> {

            IncidenceDetailsResponse detailsResponse = new IncidenceDetailsResponse();

            detailsResponse.setId(i.getId());
            detailsResponse.setAutoClosed(i.getAutoclosed());
            detailsResponse.setCreatedAt(i.getCreatedAt());
            detailsResponse.setStatus(i.getStatus());
            detailsResponse.setDecision(i.getDecision());

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
                userSimpleResponse.setGender(reporter.getGender().toString());
                userSimpleResponse.setFirstName(reporter.getFirstName());
                userSimpleResponse.setLastName(reporter.getLastName());

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

    @Override
    public ClaimIncidenceResponse claim(RequestClaimIncidence req) {

        Incidence incidence = incidenceRepository.findById(req.getIncidenceId()).orElseThrow(() -> new IncidenceNotFoundException("Incidencia no encontrada con id=" + req.getIncidenceId()));

        if (!incidence.getStatus().equals(IncidenceStatus.OPEN)) {
            throw new IncidenceNotOpenException("La incidencia no se encuentra abierta para poder ser reclamada.");
        }

        if (incidence.getModerator() != null) {
            throw new IncidenceAlreadyClaimedException("La incidencia ya fue reclamada por otro moderador.");
        }

        if (incidence.getDecision() != null) {
            throw new IncidenceAlreadyDecidedException("La incidencia ya tiene una decision tomada.");
        }

        User moderator = userRepository.findById(req.getModeratorId()).orElseThrow(() -> new ModeratorNotFoundException("Moderador no encontrado con id=" + req.getModeratorId()));
        incidence.setModerator(moderator);
        incidenceRepository.save(incidence);

        ClaimIncidenceResponse response = new ClaimIncidenceResponse();
        response.setIncidenceId(incidence.getId());
        response.setModeratorName(moderator.getFirstName() + " " + moderator.getLastName());
        response.setMessage("Incidencia reclamada exitosamente por el moderador.");

        return response;
    }
}
