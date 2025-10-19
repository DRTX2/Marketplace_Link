package com.gpis.marketplace_link.validation.validator;

import com.gpis.marketplace_link.validation.annotation.ImageCount;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ImageCountValidator implements ConstraintValidator<ImageCount, List<MultipartFile>> {

    private int min;
    private int max;

    @Override
    public void initialize(ImageCount constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(List<MultipartFile> files, ConstraintValidatorContext context) {
        if (files == null) return false;
        int size = files.size();
        return size >= min && size <= max;
    }
}
