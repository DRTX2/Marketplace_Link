package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestClaimIncidence {

    @JsonProperty("incidence_id")
    @NotNull(message = "Incidence ID cannot be null")
    private Long incidenceId;

    @JsonProperty("moderator_id")
    @NotNull(message = "Moderator ID cannot be null")
    private Long moderatorId;
}