package com.gpis.marketplace_link.services.appeal;

import com.gpis.marketplace_link.dto.appeal.AppealDetailsResponse;
import com.gpis.marketplace_link.dto.appeal.AppealSimpleResponse;
import com.gpis.marketplace_link.dto.appeal.MakeAppealDecisionRequest;

import java.util.List;

public interface AppealService {

    void autoAssignModeratorsToPendingAppeals();
    List<AppealDetailsResponse> fetchAll();
    AppealSimpleResponse makeDecision(MakeAppealDecisionRequest req);
}
