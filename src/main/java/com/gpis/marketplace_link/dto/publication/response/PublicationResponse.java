package com.gpis.marketplace_link.dto.publication.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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
        String workingHours,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime publicationDate,
        Double latitude,
        Double longitude,
        List<PublicationImageReponse> images,
        CategoryResponse category,
        VendorResponse vendor
) {

}
