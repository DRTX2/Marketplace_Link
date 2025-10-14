package com.gpis.marketplace_link.dto.publication.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicationResponse(
        Long id,
        String type,
        String name,
        BigDecimal price,
        String availability,
        LocalDateTime publicationDate,
        PublicationImageDTO image
) { }