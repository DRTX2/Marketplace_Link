package com.gpis.marketplace_link.dto.incidence;

import com.gpis.marketplace_link.enums.ReportSource;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportData {
    private String reason;
    private String comment;
    private ReportSource source;
}
