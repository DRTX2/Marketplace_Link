package com.gpis.marketplace_link.services.incidence;

import com.gpis.marketplace_link.dto.incidence.*;
import com.gpis.marketplace_link.entities.Incidence;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.entities.Report;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.enums.IncidenceStatus;
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

    private final SecurityService securityService;
    private final IncidenceRepository incidenceRepository;
    private final PublicationRepository publicationRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private static final int REPORT_THRESHOLD = 3;

    /**
     * Cierra automáticamente las incidencias que superan un tiempo determinado sin actividad.
     *
     * Por defecto, el sistema cierra incidencias que llevan más de 24 horas abiertas.
     * Este proceso se ejecuta de forma transaccional y actualiza en lote los registros
     * afectados en la base de datos.
     *
     */
    @Transactional
    @Override
    public void autoclose() {
        int hours = 24;
        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);
        int updated = incidenceRepository.bulkAutoClose(cutoff);
        log.info("Incidencias auto-cerradas: {}", updated);
    }

    /**
     * Crea o agrega un reporte de publicacion a una incidencia existente.
     *
     * Este método valida si ya existe una incidencia activa (OPEN, UNDER_REVIEW o APPEALED)
     * asociada a una publicación. Si no existe, crea una nueva incidencia y asocia el primer reporte.
     * En caso contrario, evalúa el estado actual de la incidencia y aplica las siguientes reglas:
     *
     * UNDER_REVIEW: no se permite agregar nuevos reportes.
     * APPEALED: no se permite agregar nuevos reportes.
     * RESOLVED: no se permite agregar nuevos reportes.
     * OPEN: se permite agregar nuevos reportes y actualizar la fecha del último reporte.
     *
     * Si una incidencia acumula tres o más reportes, el estado de la publicación se cambia a
     * UNDER_REVIEW y la incidencia pasa automáticamente a revisión.
     *
     * @param req objeto con los datos del reporte (ID de la publicación, ID del reportador,
     *            razon y comentario).
     *
     * @return una instancia de {@link ReportResponse} con los datos del reporte generado
     *         o agregado correctamente.
     *
     * @throws PublicationNotFoundException si la publicación asociada al reporte no existe.
     * @throws ReporterNotFoundException si el usuario reportador no existe.
     * @throws PublicationUnderReviewException si se intenta agregar un reporte a una incidencia bajo revisión.
     * @throws CannotAddReportToAppealedIncidenceException si se intenta agregar un reporte a una incidencia apelada.
     * @throws IncidenceAlreadyResolvedException si la incidencia ya está resuelta y no se pueden agregar más reportes.
     *
     * @see com.gpis.marketplace_link.entities.Incidence
     * @see com.gpis.marketplace_link.entities.Report
     * @see com.gpis.marketplace_link.dto.incidence.ReportResponse
     * @see com.gpis.marketplace_link.exceptions.business.publications.PublicationUnderReviewException
     * @see com.gpis.marketplace_link.exceptions.business.incidences.CannotAddReportToAppealedIncidenceException
     * @see com.gpis.marketplace_link.exceptions.business.incidences.IncidenceAlreadyResolvedException
     */
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
                throw new IncidenceAlreadyResolvedException("La incidencia ya fue resuelta, no se pueden agregar mas reportes.");
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

    /**
     * Trae todas las incidencias que aún no han sido revisadas por un moderador.
     * @return lista de incidencias con estado OPEN.
     */
    @Override
    public List<IncidenceDetailsResponse> fetchAllUnreviewed() {
        List<Incidence> incidences = this.incidenceRepository.findAllUnreviewedWithDetails();
       return generateIncidenteDetailResponse(incidences);
    }

    /**
     * Trae todas las incidencias que han sido reclamadas por el moderador actual.
     * @return lista de incidencias reclamadas por el moderador actual.
     */
    @Override
    public List<IncidenceDetailsResponse> fetchAllReviewed() {
        Long currentUserId = securityService.getCurrentUserId();
        List<Incidence> incidences = this.incidenceRepository.findAllReviewedWithDetails(currentUserId);
        return generateIncidenteDetailResponse(incidences);
    }

    public List<IncidenceDetailsResponse> generateIncidenteDetailResponse(List<Incidence> incidences) {
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
            }).toList();
            detailsResponse.setReports(reports);

            return detailsResponse;
        }).toList();
    }

    /**
     * Permite a un moderador reclamar una incidencia que se encuentra abierta para revisarla.
     * @param req datos de la incidencia y el moderador que la reclama.
     * @return respuesta con los detalles de la incidencia reclamada.
     * @throws IncidenceNotOpenException si la incidencia no esta abierta.
     * @throws IncidenceAlreadyClaimedException si la incidencia ya fue reclamada por otro moderador.
     * @throws IncidenceAlreadyDecidedException si la incidencia ya tiene una decision tomada.
     * @throws ModeratorNotFoundException si el moderador no existe.
     */
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
