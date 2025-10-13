package com.gpis.marketplace_link.mappers;

import com.gpis.marketplace_link.dto.role.RoleRequest;
import com.gpis.marketplace_link.dto.role.RoleResponse;
import com.gpis.marketplace_link.entities.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RoleMapper {

    RoleResponse toResponse(Role role);

    @Named("requestToRole")
    default Role requestToRole(RoleRequest req) {
        if (req == null) return null;
        Role role = new Role();
        role.setName(req.roleName());
        return role;
    }

    @Named("requestsToRoles")
    default Set<Role> requestsToRoles(Set<RoleRequest> requests) {
        if (requests == null) return null;
        return requests.stream()
                .map(this::requestToRole)
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
