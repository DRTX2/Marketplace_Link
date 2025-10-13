package com.gpis.marketplace_link.dto.user;

import com.gpis.marketplace_link.dto.role.RoleRequest;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.validation.annotation.Cedula;
import com.gpis.marketplace_link.validation.annotation.UniqueValue;
import com.gpis.marketplace_link.validation.groups.Update;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.Set;

public record UserUpdateRequest(

        @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres", groups = Update.class)
        @UniqueValue(entity = User.class, field = "username", message = "El nombre de usuario ya está registrado", groups = Update.class)
        String username,

        @Email(message = "El correo electrónico no es válido", groups = Update.class)
        @Size(max = 255, message = "El correo electrónico no debe exceder 255 caracteres", groups = Update.class)
        @UniqueValue(entity = User.class, field = "email", message = "El correo electrónico ya está registrado", groups = Update.class)
        String email,

        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres", groups = Update.class)
        String password,

        @Size(max = 20, message = "El teléfono no debe exceder 20 caracteres", groups = Update.class)
        @Pattern(regexp = "^\\+?[0-9]{7,20}$", message = "El teléfono debe ser un número válido", groups = Update.class)
        @UniqueValue(entity = User.class, field = "phone", message = "El teléfono ya está registrado", groups = Update.class)
        String phone,

        @Size(max = 100, message = "El nombre no debe exceder 100 caracteres", groups = Update.class)
        String firstName,

        @Size(max = 100, message = "El apellido no debe exceder 100 caracteres", groups = Update.class)
        String lastName,

        @Size(min = 10, max = 10, message = "La cédula debe tener exactamente 10 dígitos", groups = Update.class)
        @Cedula(message = "La cédula no sigue un formato válido de cédula ecuatoriana.", groups = Update.class)
        @UniqueValue(entity = User.class, field = "cedula", message = "La cédula ya está registrada", groups = Update.class)
        String cedula,

        @Pattern(regexp = "^(?!\\s*$).+", message = "El género no puede ser vacío", groups = Update.class)
        String gender,

        @Pattern(regexp = "^(?!\\s*$).+", message = "El estado de la cuenta no puede ser vacío", groups = Update.class)
        String accountStatus,

        @Valid
        @Size(min = 1, message = "Debe especificar al menos un rol si se envía el campo", groups = Update.class)
        Set<@Valid RoleRequest> roles

) {}
