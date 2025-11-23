package com.gpis.marketplace_link.mappers;

import com.gpis.marketplace_link.dto.publication.response.FavoritePublicationResponse;
import com.gpis.marketplace_link.dto.publication.response.VendorResponse;
import com.gpis.marketplace_link.entities.FavoritePublication;
import com.gpis.marketplace_link.entities.PublicationImage;
import com.gpis.marketplace_link.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface FavoritePublicationMapper {
    @Mapping(target = "id", source = "id")
    @Mapping(target = "publicationId", source = "publication.id")
    @Mapping(target = "code", source = "publication.code")
    @Mapping(target = "name", source = "publication.name")
    @Mapping(target = "description", source = "publication.description")
    @Mapping(target = "price", source = "publication.price")
    @Mapping(target = "type", expression = "java(favorite.getPublication().getType().name())")
    @Mapping(target = "availability", expression = "java(favorite.getPublication().getAvailability().name())")
    @Mapping(target = "status", expression = "java(favorite.getPublication().getStatus().name())")
    @Mapping(target = "publicationDate", source = "publication.publicationDate")
    @Mapping(target = "favoritedAt", source = "createdAt")
    @Mapping(target = "categoryName", expression = "java(favorite.getPublication().getCategory() != null ? favorite.getPublication().getCategory().getName() : null)")
    @Mapping(target = "vendor", expression = "java(mapVendor(favorite.getPublication().getVendor()))")
    @Mapping(target = "imageUrls", expression = "java(mapImageUrls(favorite.getPublication().getImages()))")
    FavoritePublicationResponse toResponse(FavoritePublication favorite);

    @Named("mapVendor")
    default VendorResponse mapVendor(User vendor) {
        if (vendor == null) return null;
        return new VendorResponse(
                vendor.getId(),
                vendor.getUsername()
        );
    }

    @Named("mapImageUrls")
    default List<String> mapImageUrls(List<PublicationImage> images) {
        if (images == null) return List.of();
        return images.stream().map(PublicationImage::getPath).collect(Collectors.toList());
    }
}
