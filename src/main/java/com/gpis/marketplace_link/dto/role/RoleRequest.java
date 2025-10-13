package com.gpis.marketplace_link.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RoleRequest(
        @NotBlank(message = "El rol no puede estar vacío")
        @Pattern(
                regexp = "^(ROLE_ADMIN|ROLE_MODERATOR|ROLE_SELLER|ROLE_BUYER)$",
                message = "Rol no permitido"
        )
        String roleName
) {}
