package com.gpis.marketplace_link.mappers;

import com.gpis.marketplace_link.dto.user.ModeratorCreateRequest;
import com.gpis.marketplace_link.dto.user.UserCreateRequest;
import com.gpis.marketplace_link.dto.user.UserResponse;
import com.gpis.marketplace_link.dto.user.UserUpdateRequest;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.enums.AccountStatus;
import com.gpis.marketplace_link.enums.Gender;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = { RoleMapper.class },
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserMapper {

    GeometryFactory GEO_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "requestsToRoles")
    @Mapping(target = "gender", source = "gender", qualifiedByName = "stringToGender")
    @Mapping(target = "accountStatus", source = "accountStatus", qualifiedByName = "stringToAccountStatus")
    @Mapping(target = "publications", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "emailVerifiedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "location", ignore = true)
    User toDomain(UserCreateRequest request);

    @AfterMapping
    default void setLocation(UserCreateRequest src, @MappingTarget User target) {
        if (src.latitude() != null && src.longitude() != null) {
            target.setLocation(GEO_FACTORY.createPoint(new Coordinate(src.longitude(), src.latitude())));
        }
    }

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "gender", source = "gender", qualifiedByName = "stringToGender")
    @Mapping(target = "accountStatus", ignore = true)
    @Mapping(target = "publications", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "emailVerifiedAt", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "location", ignore = true)
    User toDomain(ModeratorCreateRequest request);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "requestsToRoles")
    @Mapping(target = "gender", source = "gender", qualifiedByName = "stringToGender")
    @Mapping(target = "accountStatus", source = "accountStatus", qualifiedByName = "stringToAccountStatus")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "publications", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "emailVerifiedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "location", ignore = true)
    User toDomain(UserUpdateRequest request);

    @AfterMapping
    default void setLocation(UserUpdateRequest src, @MappingTarget User target) {
        if (src.latitude() != null && src.longitude() != null) {
            target.setLocation(GEO_FACTORY.createPoint(new Coordinate(src.longitude(), src.latitude())));
        }
    }

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToResponses")
    @Mapping(target = "gender", source = "gender", qualifiedByName = "genderToString")
    @Mapping(target = "accountStatus", source = "accountStatus", qualifiedByName = "accountStatusToString")
    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    @Mapping(target = "latitude",  expression = "java(user.getLocation() != null ? user.getLocation().getY() : null)")
    @Mapping(target = "longitude", expression = "java(user.getLocation() != null ? user.getLocation().getX() : null)")
    UserResponse toResponse(User user);

    @Named("stringToGender")
    default Gender stringToGender(String value) {
        return value == null ? null : Gender.valueOf(value.toUpperCase());
    }

    @Named("genderToString")
    default String genderToString(Gender gender) {
        return gender == null ? null : gender.name();
    }

    @Named("stringToAccountStatus")
    default AccountStatus stringToAccountStatus(String value) {
        return value == null ? null : AccountStatus.valueOf(value.toUpperCase());
    }

    @Named("accountStatusToString")
    default String accountStatusToString(AccountStatus status) {
        return status == null ? null : status.name();
    }
}
