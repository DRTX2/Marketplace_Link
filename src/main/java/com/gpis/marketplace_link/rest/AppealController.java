package com.gpis.marketplace_link.rest;

import com.gpis.marketplace_link.dto.appeal.AppealDetailsResponse;
import com.gpis.marketplace_link.dto.appeal.AppealSimpleDetailsResponse;
import com.gpis.marketplace_link.dto.appeal.AppealSimpleResponse;
import com.gpis.marketplace_link.dto.appeal.MakeAppealDecisionRequest;
import com.gpis.marketplace_link.services.appeal.AppealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/appeals")
public class AppealController {

    private final AppealService appealService;

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/my")
    public Page<AppealSimpleDetailsResponse> fetchMyAppeals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return appealService.fetchAll(pageable);
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/{appealId}")
    public AppealDetailsResponse fetchAppealDetails(@PathVariable Long appealId) {
        return appealService.fetchAppealDetails(appealId);
    }


    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @PostMapping("/decision")
    public AppealSimpleResponse makeAppealDecision(@Valid @RequestBody MakeAppealDecisionRequest req) {
        return appealService.makeDecision(req);
    }
}
