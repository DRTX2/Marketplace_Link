package com.gpis.marketplace_link.exceptions.advices;

import com.gpis.marketplace_link.exceptions.business.incidences.*;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationNotFoundException;
import com.gpis.marketplace_link.exceptions.business.publications.PublicationUnderReviewException;
import com.gpis.marketplace_link.exceptions.business.users.ModeratorNotFoundException;
import com.gpis.marketplace_link.exceptions.business.users.ReporterNotFoundException;
import com.gpis.marketplace_link.exceptions.business.publications.DangerousDictionaryLoadException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(1)
public class BusinessAdvice {

    // --- incidencias ---
    @ExceptionHandler(IncidenceNotFoundException.class)
    public ProblemDetail handleIncidenceNotFound(IncidenceNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Incidence Not Found");
        return pd;
    }

    @ExceptionHandler(IncidenceAlreadyClaimedException.class)
    public ProblemDetail handleAlreadyClaimed(IncidenceAlreadyClaimedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Incidence Already Claimed");
        return pd;
    }

    @ExceptionHandler(IncidenceAlreadyDecidedException.class)
    public ProblemDetail handleAlreadyDecided(IncidenceAlreadyDecidedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Incidence Already Decided");
        return pd;
    }

    @ExceptionHandler(IncidenceNotOpenException.class)
    public ProblemDetail handleNotOpen(IncidenceNotOpenException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Incidence Not Open");
        return pd;
    }

    @ExceptionHandler(CannotAddReportToAppealedIncidenceException.class)
    public ProblemDetail handleCannotAddReport(CannotAddReportToAppealedIncidenceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Cannot Add Report to Appealed Incidence");
        return pd;
    }

    // --- publicaciones ---
    @ExceptionHandler(PublicationNotFoundException.class)
    public ProblemDetail handlePublicationNotFound(PublicationNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Publicación no encontrada");
        return pd;
    }

    @ExceptionHandler(PublicationUnderReviewException.class)
    public ProblemDetail handlePublicationUnderReview(PublicationUnderReviewException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Publication Under Review");
        return pd;
    }

    @ExceptionHandler(DangerousDictionaryLoadException.class)
    public ProblemDetail handleDangerousDictionaryLoad(DangerousDictionaryLoadException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        pd.setTitle("Error al cargar el diccionario de palabras peligrosas");
        return pd;
    }

    // --- usuarios ---
    @ExceptionHandler(ModeratorNotFoundException.class)
    public ProblemDetail handleModeratorNotFound(ModeratorNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Moderator Not Found");
        return pd;
    }

    @ExceptionHandler(ReporterNotFoundException.class)
    public ProblemDetail handleReporterNotFound(ReporterNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Reporter Not Found");
        return pd;
    }

    @ExceptionHandler(SystemActionAlreadyTakenException.class)
    public ProblemDetail handleSystemActionAlreadyTaken(SystemActionAlreadyTakenException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("System Action Already Taken");
        return pd;
    }
}
