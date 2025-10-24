package com.gpis.marketplace_link.dto.incidence.projections;

import com.gpis.marketplace_link.enums.IncidenceDecision;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import com.gpis.marketplace_link.enums.PublicationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public interface IncidenceDetailsProjection {

    //Incidencia
    UUID getIncidencePublicUi();
    Boolean getIncidenceAutoclosed();
    LocalDateTime getIncidenceCreatedAt();
    IncidenceStatus getIncidenceStatus();
    IncidenceDecision getIncidenceDecision();
    Long getIncidenceModeratorId();
    String getIncidenceModeratorComment();

    //Publicacion
    Long getPublicationId();
    String getPublicationDescription();
    PublicationStatus getPublicationStatus();
    String getPublicationName();

    // Vendedor
    Long getPublicationVendorId();

    //Reportador
    Long getReporterId();
    String getReporterEmail();
    String getReporterFullname();

    //Reporte
    Long getReportId();
    String getReportComment();
    String getReportReason();
    LocalDateTime getReportCreatedAt();

}
