package com.gpis.marketplace_link.services.incidence;

import com.gpis.marketplace_link.dto.incidence.*;

import java.util.List;

public interface IncidenceService {

    void autoclose();
    ReportResponse report(RequestReportProduct req);
    List<IncidenceDetailsResponse> fetchAllUnreviewed();
    ClaimIncidenceResponse claim(RequestClaimIncidence req);
    //void makeDecision(RequestMakeDecision req);

}
