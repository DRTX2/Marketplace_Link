package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gpis.marketplace_link.enums.IncidenceDecision;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DecisionResponse {

    @JsonProperty("incidence_id")
    private UUID publicIncidenceUi;
    private IncidenceDecision decision;
    private String status;
    private String message;

}
