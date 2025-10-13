package com.gpis.marketplace_link.dto.publication.response;

import com.gpis.marketplace_link.entities.Category;
import com.gpis.marketplace_link.entities.PublicationImage;
import com.gpis.marketplace_link.entities.User;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PublicationResponse(
        Long id,
        String code,
        String type,
        String name,
        String description,
        BigDecimal price,
        String availability,
        LocalDateTime publicationDate,
        User vendor,
        Point locationPoint,
        String city,
        String country,
        Category category,
        List<PublicationImage> images
) { }