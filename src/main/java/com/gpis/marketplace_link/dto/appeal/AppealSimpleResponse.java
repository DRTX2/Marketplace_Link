package com.gpis.marketplace_link.dto.appeal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gpis.marketplace_link.enums.AppealDecision;
import com.gpis.marketplace_link.enums.AppealStatus;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import com.gpis.marketplace_link.enums.PublicationStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AppealSimpleResponse {

    // que decision se tomo
    @JsonProperty("final_decision")
    private AppealDecision finalDecision;

    @JsonProperty("final_decision_at")
    private LocalDateTime finalDecisionAt;

    // en que estado quedo la status;
    @JsonProperty("appeal_status")
    private AppealStatus appealStatus;

    // en que estado quedo la incidencia
    @JsonProperty("incidence_status")
    private IncidenceStatus incidenceStatus;

    // en que estado quedo la publicacion
    @JsonProperty("publication_status")
    private PublicationStatus publicationStatus;

    private String message;

}
