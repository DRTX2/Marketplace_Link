package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RequestClaimIncidence {

    @JsonProperty("incidence_id")
    @NotNull(message = "Incidence ID cannot be null")
    private UUID publicIncidenceUi;
}