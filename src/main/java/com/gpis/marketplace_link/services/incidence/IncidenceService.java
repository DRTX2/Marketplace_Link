package com.gpis.marketplace_link.services.incidence;

import com.gpis.marketplace_link.dto.incidence.*;

import java.util.List;

public interface IncidenceService {

    void autoclose();
    ReportResponse reportByUser(RequestUserReport req);
    ReportResponse reportBySystem(RequestSystemReport req);
    List<IncidenceDetailsResponse> fetchAllUnreviewed();
    List<IncidenceDetailsResponse> fetchAllReviewed();
    ClaimIncidenceResponse claim(RequestClaimIncidence req);
    //void makeDecision(RequestMakeDecision req);

}
