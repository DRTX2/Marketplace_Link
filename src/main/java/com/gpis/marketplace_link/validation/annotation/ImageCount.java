package com.gpis.marketplace_link.validation.annotation;

import com.gpis.marketplace_link.validation.validator.ImageCountValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ImageCountValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ImageCount {

    String message() default "Se debe subir entre {min} y {max} imágenes";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int min() default 1;

    int max() default 5;
}
