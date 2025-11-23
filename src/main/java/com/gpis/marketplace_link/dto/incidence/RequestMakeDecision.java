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

    @Size(min = 10, max = 255, message = "El comentario debe tener entre 10 y 255 caracteres")
    @NotNull(message = "El comentario no debe ser nulo")
    private String comment;

    @NotNull(message = "La decision no debe ser nula")
    private IncidenceDecision decision;

}
