package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SimpleReportResponse {

    private Long id;
    private String reason;
    private String comment;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    private UserSimpleResponse reporter;

}
