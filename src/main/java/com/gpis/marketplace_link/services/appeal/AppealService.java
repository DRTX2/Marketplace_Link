package com.gpis.marketplace_link.services.appeal;

import com.gpis.marketplace_link.dto.appeal.AppealDetailsResponse;
import com.gpis.marketplace_link.dto.appeal.AppealSimpleDetailsResponse;
import com.gpis.marketplace_link.dto.appeal.AppealSimpleResponse;
import com.gpis.marketplace_link.dto.appeal.MakeAppealDecisionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppealService {

    void autoAssignModeratorsToPendingAppeals();
    Page<AppealSimpleDetailsResponse> fetchAll(Pageable pageable);
    AppealDetailsResponse fetchAppealDetails(Long appealId);
    AppealSimpleResponse makeDecision(MakeAppealDecisionRequest req);
}
