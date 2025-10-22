package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class RequestAppealIncidence {

    @JsonProperty("incidence_id")
    @NotBlank(message = "Incidence Id cannot be null")
    private UUID publicIncidenceUi;

    @Size(min = 100, max = 500, message = "Appeal reason must be between 100 and 500 characters")
    @NotBlank(message = "Appeal reason cannot be blank")
    private String reason;

}
