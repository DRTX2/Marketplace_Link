package com.gpis.marketplace_link.dto.appeal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gpis.marketplace_link.enums.AppealDecision;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MakeAppealDecisionRequest {

    @NotNull(message = "AppealId must not be null")
    @JsonProperty("appeal_id")
    private Long appealId;

    @NotNull(message = "FinalDecision must not be null")
    @JsonProperty("final_decision")
    private AppealDecision finalDecision;

}
