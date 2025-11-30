package com.gpis.marketplace_link.validation.validator;

import com.gpis.marketplace_link.dto.publication.request.PublicationUpdateRequest;
import com.gpis.marketplace_link.validation.annotation.PublicationUpdateValid;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class PublicationUpdateValidator
        implements ConstraintValidator<PublicationUpdateValid, PublicationUpdateRequest> {

    @Override
    public boolean isValid(PublicationUpdateRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        List<MultipartFile> newImages = request.images();
        List<String> existingImages = request.existingImageUrls();

        // Filtrar archivos vacíos (MultipartFile vacío)
        long newImagesCount = newImages != null ? newImages.stream().filter(f -> !f.isEmpty()).count() : 0;
        int existingImagesCount = existingImages != null ? existingImages.size() : 0;

        long total = newImagesCount + existingImagesCount;

        if (total < 1 || total > 5) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("La cantidad de imágenes debe estar entre 1 y 5")
                    .addPropertyNode("images") // Asociar error al campo images para que el frontend lo muestre ahí
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
