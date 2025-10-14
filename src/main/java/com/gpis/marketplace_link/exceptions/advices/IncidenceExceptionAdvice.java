package com.gpis.marketplace_link.exceptions.advices;

import com.gpis.marketplace_link.exceptions.business.incidences.*;
import com.gpis.marketplace_link.rest.IncidenceController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maneja las excepciones de negocio relacionadas con Incidencias.
 */
@RestControllerAdvice(assignableTypes = {IncidenceController.class})
public class IncidenceExceptionAdvice {

    @ExceptionHandler(IncidenceNotFoundException.class)
    public ProblemDetail handleNotFound(IncidenceNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Incidence Not Found");
        return pd;
    }

    @ExceptionHandler(IncidenceNotOpenException.class)
    public ProblemDetail handleNotOpen(IncidenceNotOpenException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Incidence Not Open");
        return pd;
    }

    @ExceptionHandler(IncidenceAlreadyDecidedException.class)
    public ProblemDetail handleAlreadyDecided(IncidenceAlreadyDecidedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Incidence Already Decided");
        return pd;
    }

    @ExceptionHandler(IncidenceAlreadyClaimedException.class)
    public ProblemDetail handleAlreadyClaimed(IncidenceAlreadyClaimedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Incidence Already Claimed");
        return pd;
    }

    @ExceptionHandler(CannotAddReportToAppealedIncidenceException.class)
    public ProblemDetail handleCannotAddReport(CannotAddReportToAppealedIncidenceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Invalid Operation on Appealed Incidence");
        return pd;
    }
}
