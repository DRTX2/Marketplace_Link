package com.gpis.marketplace_link.dto.incidence.projections;

public interface IncidenceStatsProjection {
    Long getTotal();
    Long getUnderReview();
    Long getAppealed();
    Long getResolved();
}
