package com.gpis.marketplace_link.exceptions.advices;

import com.gpis.marketplace_link.exceptions.business.appeals.UnauthorizedAppealDecisionException;
import com.gpis.marketplace_link.exceptions.business.incidences.*;
import com.gpis.marketplace_link.exceptions.business.publications.*;
import com.gpis.marketplace_link.exceptions.business.users.ModeratorNotFoundException;
import com.gpis.marketplace_link.exceptions.business.users.ReporterNotFoundException;
import com.gpis.marketplace_link.exceptions.business.users.UserNotFoundException;
import com.gpis.marketplace_link.exceptions.business.users.*;
import jakarta.servlet.http.HttpServletRequest;
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

    @ExceptionHandler(IncidenceAlreadyClosedException.class)
    public ProblemDetail handleAlreadyClosed(IncidenceAlreadyClosedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Incidence Already Closed");
        return pd;
    }

    @ExceptionHandler(IncidenceNotClaimableException.class)
    public ProblemDetail handleNotClaimable(IncidenceNotClaimableException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Incidence Not Claimable");
        return pd;
    }

    @ExceptionHandler(SystemActionAlreadyTakenException.class)
    public ProblemDetail handleSystemActionAlreadyTaken(SystemActionAlreadyTakenException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("System Action Already Taken");
        return pd;
    }

    @ExceptionHandler(IncidenceAppealedException.class)
    public ProblemDetail handleIncidenceAppealed(IncidenceAppealedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Incidence Appealed");
        return pd;
    }

    @ExceptionHandler(IncidenceAlreadyAppealedException.class)
    public ProblemDetail handleIncidenceAlreadyAppealed(IncidenceAlreadyAppealedException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Incidence Already Appealed");
        return pd;
    }

    @ExceptionHandler(IncidenceNotUnderReviewException.class)
    public ProblemDetail handleIncidenceNotUnderReview(IncidenceNotUnderReviewException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Incidence Not Under Review");
        return pd;
    }

    @ExceptionHandler(IncidenceNotBelongToModeratorException.class)
    public ProblemDetail handleIncidenceNotBelongToModerator(IncidenceNotBelongToModeratorException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Incidence Not Belong To Moderator");
        return pd;
    }

    // --- apelaciones ---
    @ExceptionHandler(UnauthorizedAppealDecisionException.class)
    public ProblemDetail handleUnauthorizedAppealDecision(UnauthorizedAppealDecisionException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Unauthorized Appeal Decision");
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

    @ExceptionHandler(DangerousContentException.class)
    public ProblemDetail handleDangerousContent(DangerousContentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Contenido peligroso detectado");
        return pd;
    }

    @ExceptionHandler(UserIsNotVendorException.class)
    public ProblemDetail handleUserIsNotVendor(UserIsNotVendorException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Usuario no permitido");
        return pd;
    }

    @ExceptionHandler(InvalidImageFileException.class)
    public ProblemDetail handleInvalidImageFile(InvalidImageFileException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Imagen inválida");
        return pd;
    }

    @ExceptionHandler(PublicationCanNotDeleteException.class)
    public ProblemDetail handlePublicationCanNotDelete(PublicationCanNotDeleteException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("No se permite eliminar la publicación");
        return pd;
    }

    @ExceptionHandler(FavoriteAlreadyExistsException.class)
    public ProblemDetail handleFavoriteAlreadyExists(FavoriteAlreadyExistsException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Favorito ya existe");
        return pd;
    }

    @ExceptionHandler(FavoriteNotFoundException.class)
    public ProblemDetail handleFavoriteNotFound(FavoriteNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Favorito no encontrado");
        return pd;
    }

    // --- usuarios ---
    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Usuario no encontrado");
        return pd;
    }

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

    // --- usuarios (estado de cuenta) ---
    @ExceptionHandler(AccountBlockedException.class)
    public ProblemDetail handleAccountBlocked(AccountBlockedException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Cuenta bloqueada");
        pd.setType(java.net.URI.create("https://example.com/errors/account-blocked"));
        pd.setInstance(java.net.URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(AccountPendingVerificationException.class)
    public ProblemDetail handleAccountPending(AccountPendingVerificationException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Cuenta pendiente de verificación");
        pd.setType(java.net.URI.create("https://example.com/errors/account-pending"));
        pd.setInstance(java.net.URI.create(req.getRequestURI()));
        return pd;
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ProblemDetail handleAccountInactive(AccountInactiveException ex, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Cuenta inactiva");
        pd.setType(java.net.URI.create("https://example.com/errors/account-inactive"));
        pd.setInstance(java.net.URI.create(req.getRequestURI()));
        return pd;
    }

}
