package com.gpis.marketplace_link.validation.validator;

import com.gpis.marketplace_link.validation.annotation.Cedula;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DniValidator implements ConstraintValidator<Cedula, String> {
    @Override
    public boolean isValid(String dni, ConstraintValidatorContext constraintValidatorContext) {

        if (dni == null || !dni.matches("\\d{10}")) {
            return false;
        }

        int province = Integer.parseInt(dni.substring(0, 2));
        if (province < 1 || province > 24) {
            return false;
        }

        int third = Character.digit(dni.charAt(2), 10);
        if (third < 0 || third> 5) {
            return false;
        }

        int[] coef = {2,1,2,1,2,1,2,1,2};

        int add = 0;

        for (int i = 0; i < 9; i++) {
            int prod = coef[i] * Character.digit(dni.charAt(i), 10);
            if (prod >= 10) {
                prod -= 9;
            }
            add += prod;
        }

        int dv = (10 - (add % 10)) % 10;
        int ultimo = Character.digit(dni.charAt(9), 10);
        return dv == ultimo;
    }
}
