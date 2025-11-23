package com.gpis.marketplace_link.dto;

public final class Messages {

    // === REPORTES ===
    public static final String REPORT_SUCCESS ="Reporte generado exitosamente.";
    public static final String PUBLICATION_UNDER_REVIEW_CANNOT_ADD_REPORT = "La publicación ya se encuentra bajo revisión, no se puede generar otro reporte.";
    public static final String INCIDENCE_APPEALED_CANNOT_ADD_REPORT = "No se puede agregar un reporte a una incidencia apelada.";

    // === USUARIOS / REPORTEROS ===
    public static final String REPORTER_NOT_FOUND = "Reportero no encontrado con id=";
    public static final String USER_SYSTEM_NOT_FOUND = "Usuario del sistema no encontrado.";
    public static final String MODERATOR_NOT_FOUND = "Moderador no encontrado con id=";
    public static final String ACCESS_DENIED_TO_FIND_INCIDENCE = "No tienes permiso para acceder a esta incidencia";
    public static final String SELLER_NOT_OWNER_OF_PUBLICATION = "No eres el propietario de la publicacion.";
    public static final String ACCESS_DENIED_TO_CLAIM_INCIDENCE= "El usuario se encuentra bloqueado o deshabilitado, por tanto no puedes reclamar esta incidencia.";
    public static final String USER_ID_PROJECTION_NOT_FOUND = "Proyección de ID de usuario no encontrada para la incidencia con id=";
    public static final String USER_NOT_FOUND= "Usuario no encontrado con id=";

    // === PUBLICACIONES ===
    public static final String PUBLICATION_NOT_FOUND ="Publicación no encontrada con id=";
    public static final String LIGHT_PUBLICATION_NOT_FOUND= "Publicación ligera no encontrada con id=";

    // === INCIDENCIAS ===
    public static final String INCIDENCE_NOT_FOUND = "Incidencia no encontrada con id=";
    public static final String INCIDENCE_PENDING_REVIEW_CANNOT_ADD_REPORT =   "No puedes agregar un reporte a una incidencia que ya está pendiente de revisión.";
    public static final String INCIDENCE_UNDER_REVIEW_CANNOT_ADD_REPORT = "No puedes agregar un reporte a una incidencia que está en revisión.";
    public static final String INCIDENCE_PROJECTION_NOT_FOUND = "Proyección de incidencia no encontrada con id=";
    public static final String LIGHT_INCIDENCE_NOT_FOUND = "Incidencia ligera no encontrada con id=";
    public static final String SELLER_NOT_ACTIVE_CANNOT_APPEAL= "El vendedor no está activo y no puede apelar la incidencia.";
    public static final String VENDOR_ID_PROJECTION_NOT_FOUND="Proyección de ID de vendedor no encontrada para la incidencia con id=";
    public static final String ACCESS_DENIED_TO_MAKE_DECISION_INCIDENCE="El usuario se encuentra bloqueado o deshabilitado, por tanto no puedes tomar una decisión sobre esta incidencia.";

    // === BLOQUEOS ===
    public static final String REPORTER_BLOCKED_FROM_REPORTING = "No puedes reportar esta publicación hasta ";
    public static final String BLOCK_REASON_SPAM_REPORTS = "Exceso de reportes sobre la misma publicación.";
    public static final String BLOCK_ACTION_REPORT = "REPORT";
    public static final String USER_BLOCKED_FOR_SPAM_REPORTS = "Has hecho demasiados reportes sobre esta publicación.";

    // === APELACIONES ===
    public static final String INCIDENCE_NOT_APPEALABLE = "Solo se pueden apelar incidencias que hayan sido rechazadas.";
    public static final String INCIDENCE_ALREADY_APPEALED = "La incidencia ya tiene una apelación registrada.";
    public static final String APPEAL_CREATED_PENDING_MODERATOR = "Apelación creada exitosamente. Pendiente de asignación de moderador.";
    public static final String APPEAL_CREATED_SUCCESS = "Apelación creada exitosamente.";
    public static final String APPEAL_NOT_FOUND = "Apelación no encontrada con id=";
    public static final String APPEAL_USER_NOT_AUTHORIZED = "El usuario actual no está autorizado para tomar una decisión sobre esta apelación.";
    public static final String APPEAL_ALREADY_DECIDED = "Ya se ha tomado una decisión sobre esta apelación.";
    public static final String APPEAL_INVALID_INCIDENT_STATUS = "El estado de la incidencia no permite tomar una decisión sobre la apelación.";
    public static final String APPEAL_DECISION_SUCCESS = "Decisión tomada con éxito sobre la apelación.";
    public static final String MODERATOR_NOT_FOUND_WITH_ID = "Moderador no encontrado con id=";
    public static final String APPEAL_NOT_ASSIGNED= "La apelación no está asignada al moderador actual.";

    // === DECISIONES ===
    public static final String INCIDENCE_NOT_UNDER_REVIEW = "No puedes tomar una decisión porque la incidencia no está en revisión. Estado actual: ";
    public static final String INCIDENCE_NOT_BELONG_TO_MODERATOR = "La incidencia no pertenece al moderador actual.";
    public static final String DECISION_PROCESSED_SUCCESSFULLY = "Decisión procesada correctamente.";

    // === RECLAMO DE INCIDENCIAS ===
    public static final String INCIDENCE_ALREADY_CLAIMED = "La incidencia ya fue reclamada por otro moderador.";
    public static final String INCIDENCE_ALREADY_DECIDED = "La incidencia ya tiene una decisión final.";
    public static final String INCIDENCE_ALREADY_CLOSED = "La incidencia fue cerrada automáticamente y no puede reclamarse.";
    public static final String INCIDENCE_NOT_CLAIMABLE = "La incidencia no puede ser reclamada en su estado actual: ";
    public static final String INCIDENCE_CLAIMED_SUCCESSFULLY = "Incidencia reclamada exitosamente por el moderador.";

    private Messages() {}
}
