package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestAppealIncidence {

    @JsonProperty("incidence_id")
    @NotNull(message = "Incidence ID cannot be null")
    private Long incidenceId;

    @Size(min = 100, max = 500, message = "Appeal reason must be between 100 and 500 characters")
    @NotBlank(message = "Appeal reason cannot be blank")
    private String reason;

}
