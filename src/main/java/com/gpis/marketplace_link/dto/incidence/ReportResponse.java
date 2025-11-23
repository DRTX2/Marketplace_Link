package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gpis.marketplace_link.enums.PublicationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
public class ReportResponse {

    @JsonProperty("incidence_id")
    private UUID publicIncidenceUi;

    @JsonProperty("publication_id")
    private Long publicationId;

    @JsonProperty("publication_status")
    private PublicationStatus publicationStatus;
    private String message;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

}
