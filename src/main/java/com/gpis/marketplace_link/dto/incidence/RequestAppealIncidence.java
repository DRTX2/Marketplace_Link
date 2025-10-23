package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RequestAppealIncidence {

    @JsonProperty("incidence_id")
    @NotNull(message = "El id de la incidencia no puede nulo.")
    private UUID publicIncidenceUi;

    @Size(min = 100, max = 500, message = "El motivo de apelación debe tener entre 100 y 500 caracteres.")
    @NotBlank(message = "Appeal reason cannot be blank.")
    private String reason;

}
