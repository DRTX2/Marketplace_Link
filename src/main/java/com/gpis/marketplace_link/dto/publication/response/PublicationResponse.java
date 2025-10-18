package com.gpis.marketplace_link.dto.publication.response;

import com.gpis.marketplace_link.entities.Category;
import com.gpis.marketplace_link.entities.PublicationImage;
import com.gpis.marketplace_link.enums.PublicationAvailable;
import com.gpis.marketplace_link.enums.PublicationStatus;

import com.gpis.marketplace_link.enums.PublicationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PublicationResponse(

        Long id,
        String code,
        PublicationType type,
        String name,
        String description,
        BigDecimal price,
        PublicationAvailable availability,
        PublicationStatus status,
        LocalDateTime publicationDate,
        List<PublicationImageReponse> images,
        CategoryResponse category,
        VendorResponse vendor
) {

}
