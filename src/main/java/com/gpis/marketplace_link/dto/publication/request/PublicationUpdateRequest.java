package com.gpis.marketplace_link.dto.publication.request;

import com.gpis.marketplace_link.entities.Category;
import com.gpis.marketplace_link.entities.User;
import com.gpis.marketplace_link.enums.PublicationAvailable;
import com.gpis.marketplace_link.validation.annotation.Exists;
import com.gpis.marketplace_link.validation.annotation.ImageCount;
import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

public record PublicationUpdateRequest(

        @NotBlank(message = "El título es obligatorio")
        @Size(min = 5, max = 100, message = "El título debe tener entre 5 y 100 caracteres")
        String name,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 10, max = 1000, message = "La descripción debe tener entre 10 y 1000 caracteres")
        String description,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
        BigDecimal price,

        @NotNull(message = "La latitud es obligatoria")
        @DecimalMin(value = "-90.0", message = "Latitud mínima -90")
        @DecimalMax(value = "90.0", message = "Latitud máxima 90")
        Double latitude,

        @NotNull(message = "La longitud es obligatoria")
        @DecimalMin(value = "-180.0", message = "Longitud mínima -180")
        @DecimalMax(value = "180.0", message = "Longitud máxima 180")
        Double longitude,

        @NotNull(message = "La disponibilidad es obligatoria")
        PublicationAvailable availability,

        String workingHours,
        @Exists(entity = Category.class, message = "La categoría no existe")

        Long categoryId,

        @Exists(entity = User.class, message = "El vendedor no existe")
        Long vendorId,

        @ImageCount(min=1,max=5,message = "La cantidad de imágenes debe estar entre 1 y 5")
        List<MultipartFile> images,

        /**
         * URLs de imágenes existentes que se deben mantener
         * (para no borrar las imágenes que el usuario NO modificó)
         */
        List<String> existingImageUrls

) {
}
