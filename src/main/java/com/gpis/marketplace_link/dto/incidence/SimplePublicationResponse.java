package com.gpis.marketplace_link.dto.incidence;

import com.gpis.marketplace_link.enums.PublicationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimplePublicationResponse {

    private Long id;
    private String name;
    private String description;
    private PublicationStatus status;
}
