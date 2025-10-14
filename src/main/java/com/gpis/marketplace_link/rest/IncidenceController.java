package com.gpis.marketplace_link.rest;

import com.gpis.marketplace_link.dto.incidence.*;
import com.gpis.marketplace_link.services.incidence.IncidenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/incidences")
public class IncidenceController {

    private final IncidenceService incidenceService;

    @PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
    @PostMapping("/report")
    public ReportResponse report(@Valid @RequestBody RequestReportProduct req) {
        return incidenceService.report(req);
    }

    @PreAuthorize("hasRole('MODERATOR')")
    @GetMapping("/all")
    public List<IncidenceDetailsResponse> fetchAllUnreviewed() {
        return incidenceService.fetchAllUnreviewed();
    }

    @PreAuthorize("hasRole('MODERATOR')")
    @GetMapping("/my")
    public List<IncidenceDetailsResponse> fetchMyReviewed() {
        return incidenceService.fetchAllReviewed();
    }

    @PreAuthorize("hasRole('MODERATONR')")
    @PostMapping("/claim")
    public ClaimIncidenceResponse claim(@Valid @RequestBody RequestClaimIncidence req) {
        return incidenceService.claim(req);
    }

}
