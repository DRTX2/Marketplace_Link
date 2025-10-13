package com.gpis.marketplace_link.services.incidence;

import com.gpis.marketplace_link.dto.incidence.RequestReportProduct;
import com.gpis.marketplace_link.entities.Incidence;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.entities.Report;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import com.gpis.marketplace_link.exceptions.*;
import com.gpis.marketplace_link.repository.IncidenceRepository;
import com.gpis.marketplace_link.repository.PublicationRepository;
import com.gpis.marketplace_link.repository.ReportRepository;
import com.gpis.marketplace_link.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class IncidenceServiceImp implements IncidenceService {

    private final IncidenceRepository incidenceRepository;
    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    private static final int REPORT_THRESHOLD = 3;

    @Transactional
    @Override
    public void report(RequestReportProduct req) {
        // Datos para o crear la incidencia o agregar el reporte a la incidencia existente.
        Long publicationId = req.getPublicationId();
        List<IncidenceStatus> status = List.of(IncidenceStatus.OPEN, IncidenceStatus.APPEALED);
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
        } else {
            // Como existe la incidencia se considera casos como
            Incidence existingIncidence = inc.get();
            Publication savedPublication = publicationRepository.findById(publicationId).orElseThrow(() -> new PublicationNotFoundException("Publicacion no encontrada con id=" + publicationId));

            // El producto esta en revision, no se pueden agregar mas reportes.
            if (savedPublication.getStatus().equals("UNDER_REVIEW")) {
                throw new PublicationUnderReviewException("La publicacion ya esta en revision, no se pueden agregar mas reportes.");
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
                incidenceRepository.save(existingIncidence);
            }

            // Si la cantidad de reportes es mayor o igual a 3, se cambia el estado de la publicacion bajo revision.
            if (existingIncidence.getReports().size() >= REPORT_THRESHOLD) {
                savedPublication.setUnderReview();
                publicationRepository.save(savedPublication);
            }
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

}
