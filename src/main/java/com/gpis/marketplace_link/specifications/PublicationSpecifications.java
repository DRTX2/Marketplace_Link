package com.gpis.marketplace_link.specifications;

import com.gpis.marketplace_link.entities.Publication;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.Locale;

public class PublicationSpecifications {

    public static Specification<Publication> idIs(Long id) {
        return (root, query, builder) -> builder.equal(root.get("id"), id);
    }

    public static Specification<Publication> statusIs(String status) {
        return (root, query, builder) ->
                status == null ? null : builder.equal(root.get("status"), status);
    }

    public static Specification<Publication> notDeleted() {
        return (root, query, builder) ->
                builder.isNull(root.get("deletedAt"));
    }

    public static Specification<Publication> notSuspended() {

        return (root, query, builder) ->
                builder.isFalse(root.get("suspended"));
    }

    public static Specification<Publication> hasCategory(Long categoryId) {

        return (root, query, builder) ->
                categoryId == null ? null : builder.equal(root.get("category").get("id"), categoryId);

    }

    public static Specification<Publication> priceBetween(BigDecimal min, BigDecimal max) {

        return (root, query, builder) -> {

            if (min != null && max != null) {
                return builder.between(root.get("price"), min, max);
            } else if (min != null) {
                return builder.greaterThanOrEqualTo(root.get("price"), min);
            } else if (max != null) {
                return builder.lessThanOrEqualTo(root.get("price"), max);
            } else {
                return null;
            }
        };

    }

    public static Specification<Publication> withinDistance(Double lat, Double lon, Double distanceKm) {
        return (root, query, builder) -> {
            if (lat == null || lon == null || distanceKm == null) {
                return null;
            }
            double meters = Math.max(0d, distanceKm) * 1000d;

            String wktPoint = String.format(Locale.US, "SRID=4326;POINT(%f %f)", lon, lat);
            return builder.isTrue(
                builder.function(
                    "ST_DWithin",
                    Boolean.class,
                    root.get("location"),
                    builder.function("ST_GeogFromText", Object.class, builder.literal(wktPoint)),
                    builder.literal(meters)
                )
            );
        };
    }

}
