package com.gpis.marketplace_link.services.incidence;

import com.gpis.marketplace_link.dto.incidence.*;
import com.gpis.marketplace_link.entities.Incidence;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.entities.Report;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import com.gpis.marketplace_link.exceptions.business.*;
import com.gpis.marketplace_link.exceptions.business.incidences.*;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationNotFoundException;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationUnderReviewException;
import com.gpis.marketplace_link.exceptions.business.users.ModeratorNotFoundException;
import com.gpis.marketplace_link.exceptions.business.users.ReporterNotFoundException;
import com.gpis.marketplace_link.repository.IncidenceRepository;
import com.gpis.marketplace_link.repository.PublicationRepository;
import com.gpis.marketplace_link.repository.ReportRepository;
import com.gpis.marketplace_link.repository.UserRepository;
import com.gpis.marketplace_link.valueObjects.PublicationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncidenceServiceImp implements IncidenceService {

    private final IncidenceRepository incidenceRepository;
    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private static final int REPORT_THRESHOLD = 3;

    @Transactional
    @Override
    public void autoclose() {
        int hours = 24;
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        int updated = incidenceRepository.bulkAutoClose(cutoff);
        log.info("Incidencias auto-cerradas: {}", updated);
    }

    @Transactional
    @Override
    public ReportResponse report(RequestReportProduct req) {
        // Datos para o crear la incidencia o agregar el reporte a la incidencia existente.
        Long publicationId = req.getPublicationId();
        List<IncidenceStatus> status = List.of(IncidenceStatus.OPEN, IncidenceStatus.UNDER_REVIEW, IncidenceStatus.APPEALED);
        Optional<Incidence> inc = incidenceRepository.findByPublicationIdAndStatusIn(publicationId, status);

        // No existe incidencia para ese producto, entonces crear la incidencia y el reporte.
        if (inc.isEmpty()) {
            Incidence incidence = new Incidence();
            Publication savedPublication = publicationRepository.findById(publicationId).orElseThrow(() -> new PublicationNotFoundException("Publicacion no encontrada con id=" + publicationId));
            incidence.setPublication(savedPublication);
            Incidence savedIncidence = incidenceRepository.save(incidence);

            User reporter = userRepository.findById(req.getReporterId()).orElseThrow(() -> new ReporterNotFoundException("Reportador no encontrado con id=" + req.getReporterId()));
            Report report = createReportForIncidenceAndReporter(savedIncidence, reporter, req.getReason(), req.getComment());
            reportRepository.save(report);

            ReportResponse response = new ReportResponse();
            response.setIncidenceId(savedIncidence.getId());
            response.setProductId(publicationId);
            response.setMessage("Reporte generado exitosamente.");
            response.setCreatedAt(LocalDateTime.now());
            return response;

        } else {
            // Como existe la incidencia se considera casos como
            Incidence existingIncidence = inc.get();

            // El producto esta en revision, no se pueden agregar mas reportes.
            if (existingIncidence.getStatus().equals(IncidenceStatus.UNDER_REVIEW)) {
                throw new PublicationUnderReviewException("La incidencia esta en revision, no se pueden agregar mas reportes.");
            }

            // La incidencia esta apelada, no se pueden agregar mas reportes.
            if (existingIncidence.getStatus().equals(IncidenceStatus.APPEALED)) {
                throw new CannotAddReportToAppealedIncidenceException("La incidencia esta apelada, no se pueden agregar mas reportes.");
            }

            // La incidencia esta resuelta, no se pueden agregar mas reportes.
            // En teoria, nunca deberia llegar a este punto, pero por las dudas se deja el chequeo.
            if (existingIncidence.getStatus().equals(IncidenceStatus.RESOLVED)) {
                throw new BusinessException("La incidencia ya fue resuelta, no se pueden agregar mas reportes.");
            }

            // La incidencia esta abierta, se puede agregar el reporte.
            if (existingIncidence.getStatus().equals(IncidenceStatus.OPEN)) {
                User reporter = userRepository.findById(req.getReporterId()).orElseThrow(() -> new ReporterNotFoundException("Reportador no encontrado con id=" + req.getReporterId()));
                Report report = createReportForIncidenceAndReporter(existingIncidence, reporter, req.getReason(), req.getComment());
                existingIncidence.getReports().add(report);
                existingIncidence.setLastReportAt(LocalDateTime.now());
                incidenceRepository.save(existingIncidence);
            }

            // Si la cantidad de reportes es mayor o igual a 3, se cambia el estado de la publicacion bajo revision.
            if (existingIncidence.getReports().size() >= REPORT_THRESHOLD) {
                Publication savedPublication = publicationRepository.findById(publicationId).orElseThrow(() -> new PublicationNotFoundException("Publicacion no encontrada con id=" + publicationId));
                existingIncidence.setStatus(IncidenceStatus.UNDER_REVIEW);
                savedPublication.setUnderReview();
                publicationRepository.save(savedPublication);
                incidenceRepository.save(existingIncidence);
            }

            ReportResponse response = new ReportResponse();
            response.setIncidenceId(existingIncidence.getId());
            response.setProductId(publicationId);
            response.setMessage("Reporte agregado exitosamente.");
            response.setCreatedAt(LocalDateTime.now());
            return response;
        }
    }

    private Report createReportForIncidenceAndReporter(Incidence incidence, User reporter,  String reason, String comment) {
        Report report = new Report();
        report.setIncidence(incidence);
        report.setReason(reason);
        report.setComment(comment);
        report.setReporter(reporter);
        return report;
    }

    @Override
    public List<IncidenceDetailsResponse> fetchAllUnreviewed() {

        List<Incidence> incidences = this.incidenceRepository.findAllUnreviewedWithDetails();

        return incidences.stream().map((i) -> {

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
            List<SimpleReportResponse> reports = i.getReports().stream().map((r) -> {

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
            }).collect(Collectors.toList());
            detailsResponse.setReports(reports);

            return detailsResponse;
        }).collect(Collectors.toList());
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
