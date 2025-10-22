package com.gpis.marketplace_link.dto.user;

import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.validation.annotation.Cedula;
import com.gpis.marketplace_link.validation.annotation.UniqueValue;
import jakarta.validation.constraints.*;

public record ModeratorCreateRequest(

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
        @UniqueValue(entity = User.class, field = "username", message = "El nombre de usuario ya está registrado")
        String username,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no es válido")
        @Size(max = 255, message = "El correo electrónico no debe exceder 255 caracteres")
        @UniqueValue(entity = User.class, field = "email", message = "El correo electrónico ya está registrado")
        String email,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 20, message = "El teléfono no debe exceder 20 caracteres")
        @Pattern(regexp = "^\\+?\\d{7,20}$", message = "El teléfono debe ser un número válido")
        @UniqueValue(entity = User.class, field = "phone", message = "El teléfono ya está registrado")
        String phone,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no debe exceder 100 caracteres")
        String lastName,

        @NotBlank(message = "La cédula es obligatoria")
        @Size(min = 10, max = 10, message = "La cédula debe tener exactamente 10 dígitos")
        @Cedula(message = "La cédula no sigue un formato válido de cédula ecuatoriana.")
        @UniqueValue(entity = User.class, field = "cedula", message = "La cédula ya está registrada")
        String cedula,

        @NotNull(message = "El género es obligatorio")
        String gender
) {
}

