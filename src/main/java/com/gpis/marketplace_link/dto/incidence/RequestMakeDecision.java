package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gpis.marketplace_link.enums.IncidenceDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RequestMakeDecision {

    @JsonProperty("incidence_id")
    private UUID publicIncidenceUi;

    @Size(min = 10, max = 255)
    @NotNull(message = "Comment must not be null")
    private String comment;

    @NotNull(message = "Decision must not be null")
    private IncidenceDecision decision;

}
