package com.gpis.marketplace_link.rest;

import com.gpis.marketplace_link.dto.appeal.AppealDetailsResponse;
import com.gpis.marketplace_link.dto.appeal.AppealSimpleResponse;
import com.gpis.marketplace_link.dto.appeal.MakeAppealDecisionRequest;
import com.gpis.marketplace_link.services.appeal.AppealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/appeals")
public class AppealController {

    private final AppealService appealService;

    @PreAuthorize("hasRole('MODERATOR')")
    @GetMapping("/my")
    public List<AppealDetailsResponse> fetchMyAppeals() {
        return appealService.fetchAll();
    }

    @PreAuthorize("hasRole('MODERATOR')")
    @PostMapping("/decision")
    public AppealSimpleResponse makeAppealDecision(@Valid @RequestBody MakeAppealDecisionRequest req) {
        return appealService.makeDecision(req);
    }

}
