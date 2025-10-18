package com.gpis.marketplace_link.mappers;

import com.gpis.marketplace_link.dto.publication.response.PublicationImageReponse;
import com.gpis.marketplace_link.dto.publication.response.PublicationResponse;
import com.gpis.marketplace_link.dto.publication.response.PublicationSummaryResponse;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.entities.PublicationImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PublicationMapper {


    PublicationResponse toResponse(Publication publication);


    @Mapping(target = "image", expression = "java(firstImage(publication))")
    PublicationSummaryResponse toSummaryResponse(Publication publication);



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
}
