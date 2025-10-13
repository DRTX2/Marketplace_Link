package com.gpis.marketplace_link.mappers;

import com.gpis.marketplace_link.dto.user.UserCreateRequest;
import com.gpis.marketplace_link.dto.user.UserResponse;
import com.gpis.marketplace_link.dto.user.UserUpdateRequest;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.valueObjects.AccountStatus;
import com.gpis.marketplace_link.valueObjects.Gender;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = { RoleMapper.class },
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "requestsToRoles")
    @Mapping(target = "gender", source = "gender", qualifiedByName = "stringToGender")
    @Mapping(target = "accountStatus", source = "accountStatus", qualifiedByName = "stringToAccountStatus")
    @Mapping(target = "publications", ignore = true) //De momento
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "emailVerifiedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    User toDomain(UserCreateRequest request);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "requestsToRoles")
    @Mapping(target = "gender", source = "gender", qualifiedByName = "stringToGender")
    @Mapping(target = "accountStatus", source = "accountStatus", qualifiedByName = "stringToAccountStatus")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "publications", ignore = true) //De momento
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "emailVerifiedAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    User toDomain(UserUpdateRequest request);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToResponses")
    @Mapping(target = "gender", source = "gender", qualifiedByName = "genderToString")
    @Mapping(target = "accountStatus", source = "accountStatus", qualifiedByName = "accountStatusToString")
    @Mapping(target = "fullName", expression = "java(user.getFullName())")
    UserResponse toResponse(User user);

    @Named("stringToGender")
    default Gender stringToGender(String value) {
        if (value == null) return null;
        return Gender.valueOf(value.toUpperCase());
    }

    @Named("genderToString")
    default String genderToString(Gender gender) {
        return gender == null ? null : gender.name();
    }

    @Named("stringToAccountStatus")
    default AccountStatus stringToAccountStatus(String value) {
        if (value == null) return null;
        return AccountStatus.valueOf(value.toUpperCase());
    }

    @Named("accountStatusToString")
    default String accountStatusToString(AccountStatus status) {
        return status == null ? null : status.name();
    }
}
