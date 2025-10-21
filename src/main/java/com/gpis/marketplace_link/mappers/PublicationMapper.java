package com.gpis.marketplace_link.mappers;

import com.gpis.marketplace_link.dto.publication.request.PublicationCreateRequest;
import com.gpis.marketplace_link.dto.publication.request.PublicationUpdateRequest;
import com.gpis.marketplace_link.dto.publication.response.PublicationImageReponse;
import com.gpis.marketplace_link.dto.publication.response.PublicationResponse;
import com.gpis.marketplace_link.dto.publication.response.PublicationSummaryResponse;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.entities.PublicationImage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface PublicationMapper {

    @Mapping(target = "latitude", expression = "java(extractLatitude(publication))")
    @Mapping(target = "longitude", expression = "java(extractLongitude(publication))")
    PublicationResponse toResponse(Publication publication);

    @Mapping(target = "image", expression = "java(firstImage(publication))")
    PublicationSummaryResponse toSummaryResponse(Publication publication);


    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "location", expression = "java(toPoint(request.latitude(), request.longitude()))")
    Publication toEntity(PublicationCreateRequest request);

    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "location", expression = "java(toPoint(request.latitude(), request.longitude()))")
    void updateFromRequest(@MappingTarget Publication publication, PublicationUpdateRequest request);


    default PublicationImageReponse toPublicationImageResponse(PublicationImage image) {
        if (image == null) return null;
        return new PublicationImageReponse(image.getId(), image.getPath());
    }


    default PublicationImageReponse firstImage(Publication publication) {
        if (publication == null) return null;
        List<PublicationImage> images = publication.getImages();
        if (images == null || images.isEmpty()) return null;
        return toPublicationImageResponse(images.get(0));
    }


    GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);


    default Point toPoint(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) return null;
        Point p = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude)); // (x=lon, y=lat)
        p.setSRID(4326);
        return p;
    }


    @AfterMapping
    default void ensureImagesInitialized(@MappingTarget Publication publication) {
        if (publication != null && publication.getImages() == null) {
            publication.setImages(new ArrayList<>());
        }
    }

    default Double extractLatitude(Publication publication) {
        if (publication == null || publication.getLocation() == null) return null;
        return publication.getLocation().getY();
    }

    default Double extractLongitude(Publication publication) {
        if (publication == null || publication.getLocation() == null) return null;
        return publication.getLocation().getX();
    }
}
