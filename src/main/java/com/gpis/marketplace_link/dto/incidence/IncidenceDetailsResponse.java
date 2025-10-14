package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gpis.marketplace_link.enums.Decision;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class IncidenceDetailsResponse {

    private Long id;
    private IncidenceStatus status;
    private Decision decision;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("auto_closed")
    private Boolean autoClosed;

    // Publicacion...
    private SimplePublicationResponse publication;

    // Reportes...
    private List<SimpleReportResponse> reports;


}
