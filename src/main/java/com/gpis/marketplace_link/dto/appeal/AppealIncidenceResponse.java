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

@Getter
@Setter
public class AppealIncidenceResponse {

    private Long id;

    @JsonProperty("moderator_comment")
    private String moderatorComment;

    private IncidenceStatus status;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("last_report_at")
    private LocalDateTime lastReportAt;

    @JsonProperty("previous_moderator")
    private ModeratorInfo previousModerator;

    private SimplePublicationResponse publication;

    private List<SimpleReportResponse> reports;

}
