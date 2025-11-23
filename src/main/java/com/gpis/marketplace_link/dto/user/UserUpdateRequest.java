package com.gpis.marketplace_link.dto.user;

import com.gpis.marketplace_link.dto.role.RoleRequest;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.validation.annotation.Cedula;
import com.gpis.marketplace_link.validation.annotation.UniqueValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.Set;

public record UserUpdateRequest(

        @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
        @UniqueValue(entity = User.class, field = "username", message = "El nombre de usuario ya está registrado")
        String username,

        @Email(message = "El correo electrónico no es válido")
        @Size(max = 255, message = "El correo electrónico no debe exceder 255 caracteres")
        @UniqueValue(entity = User.class, field = "email", message = "El correo electrónico ya está registrado")
        String email,

        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password,

        @Size(max = 20, message = "El teléfono no debe exceder 20 caracteres")
        @Pattern(regexp = "^\\+?\\d{7,20}$", message = "El teléfono debe ser un número válido")
        @UniqueValue(entity = User.class, field = "phone", message = "El teléfono ya está registrado")
        String phone,

        @Size(max = 100, message = "El nombre no debe exceder 100 caracteres")
        String firstName,

        @Size(max = 100, message = "El apellido no debe exceder 100 caracteres")
        String lastName,

        @Size(min = 10, max = 10, message = "La cédula debe tener exactamente 10 dígitos")
        @Cedula(message = "La cédula no sigue un formato válido de cédula ecuatoriana.")
        @UniqueValue(entity = User.class, field = "cedula", message = "La cédula ya está registrada")
        String cedula,

        @Pattern(regexp = "^(?!\\s*$).+", message = "El género no puede ser vacío")
        String gender,

        @Pattern(regexp = "^(?!\\s*$).+", message = "El estado de la cuenta no puede ser vacío")
        String accountStatus,

        @Valid
        @Size(min = 1, message = "Debe especificar al menos un rol si se envía el campo")
        Set<@Valid RoleRequest> roles,

        @DecimalMin(value = "-90.0", message = "La latitud mínima es -90")
        @DecimalMax(value = "90.0",  message = "La latitud máxima es 90")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "La longitud mínima es -180")
        @DecimalMax(value = "180.0",  message = "La longitud máxima es 180")
        Double longitude

) {
    @AssertTrue(message = "Debe enviar ambos: latitude y longitude, o ninguno")
    public boolean isValidLocationForUpdate() {
        return (latitude == null && longitude == null) ||
                (latitude != null && longitude != null);
    }
}
