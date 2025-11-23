package com.gpis.marketplace_link.services.incidence;

import com.gpis.marketplace_link.dto.incidence.AppealResponse;
import com.gpis.marketplace_link.dto.incidence.*;
import com.gpis.marketplace_link.enums.IncidenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public interface IncidenceService {

    IncidenceStatsResponse fetchStatsByUserId(Long userId);
    void autoclose();
    ReportResponse reportByUser(RequestUserReport req);
    ReportResponse reportBySystem(RequestSystemReport req);
    Page<IncidenceSimpleDetailsResponse> fetchAllUnreviewed(Pageable pageable, LocalDateTime startDate, LocalDateTime endDate);
    Page<IncidenceSimpleDetailsResponse> fetchAllReviewed(Pageable pageable, LocalDateTime startDate, LocalDateTime endDate);
    IncidenceDetailsResponse fetchByPublicUiNativeProjection(UUID publicUi);
    IncidenceDetailsResponse fetchByPublicUiForSellerNativeProjection(UUID publicUi);
    ClaimIncidenceResponse claim(RequestClaimIncidence req);
    DecisionResponse makeDecision(RequestMakeDecision req);
    AppealResponse appeal(RequestAppealIncidence req);

}
