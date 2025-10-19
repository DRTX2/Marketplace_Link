package com.gpis.marketplace_link.jobs;

import com.gpis.marketplace_link.services.appeal.AppealService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppealAutoAssigner {

    private final AppealService appealService;

    @Scheduled(cron = "0 30 3 * * *")
    public void assignPendingModerators() {
        appealService.autoAssignModeratorsToPendingAppeals();
    }
}
