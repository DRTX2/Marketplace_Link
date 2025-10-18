package com.gpis.marketplace_link.dto.publication.response;

import com.gpis.marketplace_link.enums.PublicationAvailable;
import com.gpis.marketplace_link.enums.PublicationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicationSummaryResponse(
        Long id,
        PublicationType type,
        String name,
        BigDecimal price,
        PublicationAvailable availability,
        LocalDateTime publicationDate,
        PublicationImageReponse image
) { }