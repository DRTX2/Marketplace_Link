package com.gpis.marketplace_link.rest;

import com.gpis.marketplace_link.dto.incidence.AppealResponse;
import com.gpis.marketplace_link.dto.incidence.*;
import com.gpis.marketplace_link.security.service.SecurityService;
import com.gpis.marketplace_link.services.incidence.IncidenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/incidences")
public class IncidenceController {

    private final IncidenceService incidenceService;
    private final SecurityService securityService;

    @GetMapping("/stats")
    public IncidenceStatsResponse fetchStprats() {
        Long userId = securityService.getCurrentUserId();
        return incidenceService.fetchStatsByUserId(userId);
    }

    @PostMapping("/system-report")
    public ReportResponse reportBySystem(@RequestBody RequestSystemReport req) {
        return incidenceService.reportBySystem(req);
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/all")
    public Page<IncidenceSimpleDetailsResponse> fetchAllUnreviewed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return incidenceService.fetchAllUnreviewed(pageable);
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/my")
    public Page<IncidenceSimpleDetailsResponse> fetchMyReviewed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return incidenceService.fetchAllReviewed(pageable);
    }

    // El usuario piensa que es id, pero en realidad se refiere al publicUi y asi se evita exponer ids secuenciales
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @GetMapping("/{publicUi}")
    public IncidenceDetailsResponse fetchById(@PathVariable UUID publicUi) {
        return incidenceService.fetchByPublicUiNativeProjection(publicUi);
    }

    @PreAuthorize("hasRole('SELLER')")
    @GetMapping("/s/{publicUi}")
    public IncidenceDetailsResponse fetchByIdForSeller(@PathVariable UUID publicUi) {
        return incidenceService.fetchByPublicUiForSellerNativeProjection(publicUi);
    }

    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    @PostMapping("/report")
    public ReportResponse report(@Valid @RequestBody RequestUserReport req) {
        return incidenceService.reportByUser(req);
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @PostMapping("/claim")
    public ClaimIncidenceResponse claim(@Valid @RequestBody RequestClaimIncidence req) {
        return incidenceService.claim(req);
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @PostMapping("/decision")
    public DecisionResponse makeDecision(@Valid @RequestBody RequestMakeDecision req) {
        return incidenceService.makeDecision(req);
    }

    @PreAuthorize("hasAnyRole('SELLER')")
    @PostMapping("/appeal")
    public AppealResponse appealIncidence(@Valid @RequestBody RequestAppealIncidence req) {
        return incidenceService.appeal(req);
    }

}
