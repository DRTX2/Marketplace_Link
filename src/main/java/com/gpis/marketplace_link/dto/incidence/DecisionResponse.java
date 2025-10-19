package com.gpis.marketplace_link.dto.incidence;

import com.gpis.marketplace_link.enums.IncidenceDecision;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class DecisionResponse {

    private Long incidenceId;
    private IncidenceDecision decision;
    private String status;
    private String message;

}
