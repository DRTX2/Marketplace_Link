package com.gpis.marketplace_link.services.incidence;

import com.gpis.marketplace_link.dto.incidence.AppealResponse;
import com.gpis.marketplace_link.dto.incidence.*;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface IncidenceService {

    IncidenceStatsResponse fetchStatsByUserId(Long userId);
    void autoclose();
    ReportResponse reportByUser(RequestUserReport req);
    ReportResponse reportBySystem(RequestSystemReport req);
    Page<IncidenceSimpleDetailsResponse> fetchAllUnreviewed(Pageable pageable);
    IncidenceDetailsResponse fetchByPublicUiNativeProjection(UUID publicUi);
    IncidenceDetailsResponse fetchByPublicUiForSellerNativeProjection(UUID publicUi);
    Page<IncidenceSimpleDetailsResponse> fetchAllReviewed(Pageable pageable);
    ClaimIncidenceResponse claim(RequestClaimIncidence req);
    DecisionResponse makeDecision(RequestMakeDecision req);
    AppealResponse appeal(RequestAppealIncidence req);

}
