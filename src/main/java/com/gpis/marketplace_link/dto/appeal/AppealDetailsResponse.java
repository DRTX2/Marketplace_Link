package com.gpis.marketplace_link.dto.appeal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gpis.marketplace_link.dto.incidence.ModeratorInfo;
import com.gpis.marketplace_link.dto.incidence.UserSimpleResponse;
import com.gpis.marketplace_link.enums.AppealDecision;
import com.gpis.marketplace_link.enums.AppealStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AppealDetailsResponse {

    // datos de la apelacion
    private Long id;
    private AppealStatus status;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("final_decision")
    private AppealDecision finalDecision;

    // datos del nuevo moderador
    @JsonProperty("new_moderator")
    private ModeratorInfo newModerator;

    // datos del vendedor que apelo
    private UserSimpleResponse seller;

    // datos de la incidencia
    private AppealIncidenceResponse incidence;

}
