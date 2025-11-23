package com.gpis.marketplace_link.dto.incidence.projections;

import java.util.UUID;

public interface IncidencePublicationProjection {

    Long getIncidenceId();
    UUID getIncidencePublicUi();
    Long getPublicationId();
    String getPublicationName();
    Long getVendorId();
    String getVendorEmail();
    String getVendorFullname();
}
