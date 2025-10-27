package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IncidenceStatsResponse {

    private Long total;

    @JsonProperty("under_review")
    private Long underReview;

    private Long appealed;
    private Long resolved;
}
