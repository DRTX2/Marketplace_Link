package com.gpis.marketplace_link.mappers;

import com.gpis.marketplace_link.dto.role.RoleResponse;
import com.gpis.marketplace_link.entities.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse toResponse(Role role);

    @Named("nameToRole")
    default Role nameToRole(String name) {
        if (name == null) return null;
        Role role = new Role();
        role.setName(name);
        return role;
    }

    @Named("namesToRoles")
    default Set<Role> namesToRoles(Set<String> names) {
        if (names == null) return null;
        return names.stream()
                .map(this::nameToRole)
                .collect(Collectors.toSet());
    }

    @Named("rolesToResponses")
    default Set<RoleResponse> rolesToResponses(Set<Role> roles) {
        if (roles == null) return null;
        return roles.stream()
                .map(this::toResponse)
                .collect(Collectors.toSet());
    }
}
