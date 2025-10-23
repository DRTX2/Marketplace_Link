package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gpis.marketplace_link.enums.IncidenceDecision;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class IncidenceSimpleDetailsResponse {

    @JsonProperty("incidence_id")
    private UUID publicIncidenceUi;

    private IncidenceStatus status;

    @JsonProperty("incidence_decision")
    private IncidenceDecision incidenceDecision;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("auto_closed")
    private Boolean autoClosed;

    private SimplePublicationResponse publication;

}
