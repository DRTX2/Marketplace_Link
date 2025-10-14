package com.gpis.marketplace_link.mappers;

import com.gpis.marketplace_link.dto.publication.response.PublicationImageDTO;
import com.gpis.marketplace_link.dto.publication.response.PublicationResponse;
import com.gpis.marketplace_link.entities.Publication;
import com.gpis.marketplace_link.entities.PublicationImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PublicationMapper {

    @Mapping(target = "image", expression = "java(firstImage(publication))")
    PublicationResponse toResponse(Publication publication);

    default PublicationImageDTO toPublicationImageDTO(PublicationImage image) {
        if (image == null) return null;
        return new PublicationImageDTO(image.getId(), image.getPath());
    }

    default PublicationImageDTO firstImage(Publication publication) {
        if (publication == null) return null;
        List<PublicationImage> images = publication.getImages();
        if (images == null || images.isEmpty()) return null;
        return toPublicationImageDTO(images.get(0));
    }
}
