package com.gpis.marketplace_link.dto.publication.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoritePublicationResponse {
    private Long id;
    private Long publicationId;
    private String code;
    private String name;
    private String description;
    private BigDecimal price;
    private String type;
    private String availability;
    private String status;
    private LocalDateTime publicationDate;
    private LocalDateTime favoritedAt;
    private String categoryName;
    private VendorResponse vendor;
    private List<String> imageUrls;
}

