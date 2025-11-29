package com.gpis.marketplace_link.dto.user;

import com.gpis.marketplace_link.dto.role.RoleResponse;
import java.util.Set;

public record UserResponse(
        Long id,
        String username,
        String email,
        String phone,
        String firstName,
        String lastName,
        String fullName,
        String cedula,
        String gender,
        String accountStatus,
        Set<RoleResponse> roles
) {}
