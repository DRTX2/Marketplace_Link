package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gpis.marketplace_link.enums.AppealStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
@Getter
@Setter
public class AppealResponse {

    @JsonProperty("appeal_id")
    private Long appealId;

    @JsonProperty("incidence_id")
    private UUID publicIncidenceUi;
    private String message;
    private AppealStatus status;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("previous_moderator")
    private ModeratorInfo previousModerator;

    @JsonProperty("new_moderator")
    private ModeratorInfo newModerator;

}
