package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gpis.marketplace_link.enums.IncidenceDecision;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class IncidenceDetailsResponse {

    @JsonProperty("incidence_id")
    private UUID publicIncidenceUi;

    private IncidenceStatus status;

    @JsonProperty("incidence_decision")
    private IncidenceDecision incidenceDecision;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("auto_closed")
    private Boolean autoClosed;

    @JsonProperty("moderator_comment")
    private String moderatorComment;

    // Publicacion...
    private SimplePublicationResponse publication;

    // Reportes...
    private List<SimpleReportResponse> reports;


}
