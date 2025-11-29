package com.gpis.marketplace_link.dto.user;

import com.gpis.marketplace_link.validation.annotation.Cedula;
import com.gpis.marketplace_link.validation.groups.Create;
import jakarta.validation.constraints.*;

import java.util.Set;

public record UserRequest(

        @NotBlank(message = "El nombre de usuario es obligatorio", groups = Create.class)
        @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
        String username,

        @NotBlank(message = "El correo electrónico es obligatorio", groups = Create.class)
        @Email(message = "El correo electrónico no es válido")
        @Size(max = 255, message = "El correo electrónico no debe exceder 255 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria", groups = Create.class)
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "El teléfono es obligatorio", groups = Create.class)
        @Size(max = 20, message = "El teléfono no debe exceder 20 caracteres")
        @Pattern(regexp = "^\\+?[0-9]{7,20}$", message = "El teléfono debe ser un número válido")
        String phone,

        @NotBlank(message = "El nombre es obligatorio", groups = Create.class)
        @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio", groups = Create.class)
        @Size(max = 100, message = "El apellido no debe exceder 100 caracteres")
        String lastName,

        @NotBlank(message = "La cédula es obligatoria", groups = Create.class)
        @Size(min = 10, max = 10, message = "La cédula debe tener exactamente 10 dígitos")
        @Cedula(message = "La cédula no sigue un formato válido de cédula ecuatoriana.")
        String cedula,

        @NotNull(message = "El género es obligatorio", groups = Create.class)
        String gender,

        @NotNull(message = "El estado de la cuenta es obligatorio", groups = Create.class)
        String accountStatus,

        @NotEmpty(message = "Debe especificar al menos un rol", groups = Create.class)
        Set<
                @NotBlank(message = "El rol no puede estar vacío")
                @Pattern(
                        regexp = "^(ROLE_ADMIN|ROLE_MODERATOR|ROLE_SELLER|ROLE_BUYER)$",
                        message = "Rol no permitido"
                )
                        String
                > roles

) {}
