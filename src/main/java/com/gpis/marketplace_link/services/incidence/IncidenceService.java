package com.gpis.marketplace_link.services.incidence;

import com.gpis.marketplace_link.dto.incidence.AppealResponse;
import com.gpis.marketplace_link.dto.incidence.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IncidenceService {

    void autoclose();
    ReportResponse reportByUser(RequestUserReport req);
    ReportResponse reportBySystem(RequestSystemReport req);
    Page<IncidenceDetailsResponse> fetchAllUnreviewed(Pageable pageable);
    Page<IncidenceDetailsResponse> fetchAllReviewed(Pageable pageable);
    ClaimIncidenceResponse claim(RequestClaimIncidence req);
    DecisionResponse makeDecision(RequestMakeDecision req);
    AppealResponse appeal(RequestAppealIncidence req);

}
