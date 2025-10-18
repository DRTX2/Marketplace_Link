package com.gpis.marketplace_link.dto.incidence;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestSystemReport {

    private Long publicationId;
    private String reason;
    private String comment;

}
