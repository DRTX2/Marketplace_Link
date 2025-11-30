package com.gpis.marketplace_link.validation.annotation;

import com.gpis.marketplace_link.validation.validator.PublicationUpdateValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PublicationUpdateValidator.class)
public @interface PublicationUpdateValid {
    String message() default "La publicación debe tener entre 1 y 5 imágenes";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
