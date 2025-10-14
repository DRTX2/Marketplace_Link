package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClaimIncidenceResponse {

    @JsonProperty("incidence_id")
    private Long incidenceId;

    private String message;
    private String moderatorName;

}
