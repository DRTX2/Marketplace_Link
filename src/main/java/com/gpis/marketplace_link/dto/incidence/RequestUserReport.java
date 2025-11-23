package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestUserReport {

    @NotNull(message = "El identificador de la publicacion es requerido.")
    @JsonProperty("publication_id")
    private Long publicationId;

    @Size(min = 3, max = 30, message = "El motivo debe tener entre 10 y maximo 30 caracteres.")
    @NotBlank(message = "El motivo es requerido.")
    private String reason;

    @Size(max = 255, message = "El comentario debe tener como maximo 255 caracteres.")
    @NotBlank(message = "El campo comentario es requerido.")
    private String comment;

}
