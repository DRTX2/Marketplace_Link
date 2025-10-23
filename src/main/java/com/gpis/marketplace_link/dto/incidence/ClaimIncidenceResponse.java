package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ClaimIncidenceResponse {

    @JsonProperty("incidence_id")
    private UUID publicIncidenceUi;

    private String message;

    @JsonProperty("moderator_name")
    private String moderatorName;

}
