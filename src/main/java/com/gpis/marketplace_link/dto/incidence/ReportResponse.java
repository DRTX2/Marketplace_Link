package com.gpis.marketplace_link.dto.incidence;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
public class ReportResponse {

    private Long incidenceId;
    private Long productId;
    private String message;
    private LocalDateTime createdAt;

}
