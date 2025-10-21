package com.gpis.marketplace_link.services.appeal;

import com.gpis.marketplace_link.dto.appeal.AppealDetailsResponse;
import com.gpis.marketplace_link.dto.appeal.AppealSimpleResponse;
import com.gpis.marketplace_link.dto.appeal.MakeAppealDecisionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AppealService {

    void autoAssignModeratorsToPendingAppeals();
    Page<AppealDetailsResponse> fetchAll(Pageable pageable);
    AppealSimpleResponse makeDecision(MakeAppealDecisionRequest req);
}
