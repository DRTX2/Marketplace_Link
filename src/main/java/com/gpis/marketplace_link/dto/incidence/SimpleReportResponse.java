package com.gpis.marketplace_link.dto.incidence;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimpleReportResponse {

    private Long id;
    private String reason;
    private String comment;
    private UserSimpleResponse reporter;

}
