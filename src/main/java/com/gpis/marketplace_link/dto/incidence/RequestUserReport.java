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

    @NotNull(message = "Publication ID is required")
    @JsonProperty("publication_id")
    private Long publicationId;

    @Size(min = 10, max = 100, message = "Reason must be between 10 and 100 characters")
    @NotBlank(message = "Reason is required")
    private String reason;

    @Size(max = 255, message = "Comment must be at most 255 characters")
    @NotBlank(message = "Comment is required")
    private String comment;

}
