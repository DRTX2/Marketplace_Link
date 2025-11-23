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
    @NotNull(message = "El id de la incidencia no puede ser nulo")
    private UUID publicIncidenceUi;
}