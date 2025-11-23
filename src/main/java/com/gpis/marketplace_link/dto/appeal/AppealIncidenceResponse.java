package com.gpis.marketplace_link.dto.appeal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gpis.marketplace_link.dto.incidence.ModeratorInfo;
import com.gpis.marketplace_link.dto.incidence.SimplePublicationResponse;
import com.gpis.marketplace_link.dto.incidence.SimpleReportResponse;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AppealIncidenceResponse {

    @JsonProperty("incidence_id")
    private UUID publicIncidenceUi;

    @JsonProperty("moderator_comment")
    private String moderatorComment;

    private IncidenceStatus status;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("previous_moderator")
    private ModeratorInfo previousModerator;

    private SimplePublicationResponse publication;

    private List<SimpleReportResponse> reports;

}
