package com.gpis.marketplace_link.rest;

import com.gpis.marketplace_link.dto.incidence.AppealResponse;
import com.gpis.marketplace_link.dto.incidence.*;
import com.gpis.marketplace_link.services.incidence.IncidenceService;
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
@RequestMapping("/api/incidences")
public class IncidenceController {

    private final IncidenceService incidenceService;

    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    @PostMapping("/report")
    public ReportResponse report(@Valid @RequestBody RequestUserReport req) {
        return incidenceService.reportByUser(req);
    }

    // Solo para test
    @PostMapping("/system-report")
    public ReportResponse systemReport(@Valid @RequestBody RequestSystemReport req) {
        return incidenceService.reportBySystem(req);
    }

    @PreAuthorize("hasRole('MODERATOR')")
    @GetMapping("/all")
    public Page<IncidenceDetailsResponse> fetchAllUnreviewed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return incidenceService.fetchAllUnreviewed(pageable);
    }

    @PreAuthorize("hasRole('MODERATOR')")
    @GetMapping("/my")
    public Page<IncidenceDetailsResponse> fetchMyReviewed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return incidenceService.fetchAllReviewed(pageable);
    }

    @PreAuthorize("hasRole('MODERATOR')")
    @PostMapping("/claim")
    public ClaimIncidenceResponse claim(@Valid @RequestBody RequestClaimIncidence req) {
        return incidenceService.claim(req);
    }

    @PreAuthorize("hasRole('MODERATOR')")
    @PostMapping("/decision")
    public DecisionResponse makeDecision(@Valid @RequestBody RequestMakeDecision req) {
        return incidenceService.makeDecision(req);
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("/appeal")
    public AppealResponse appealIncidence(@Valid @RequestBody RequestAppealIncidence req) {
        return incidenceService.appeal(req);
    }

}
