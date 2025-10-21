package com.gpis.marketplace_link.validation.validator;

import com.gpis.marketplace_link.validation.annotation.Cedula;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DniValidator implements ConstraintValidator<Cedula, String> {

    @Override
    public boolean isValid(String dni, ConstraintValidatorContext context) {
        if (dni == null || dni.isBlank()) {
            return true;
        }

        dni = dni.trim();
        if (!dni.matches("\\d{10}")) {
            return false;
        }

        int province = Integer.parseInt(dni.substring(0, 2));
        if (province < 1 || province > 24) {
            return false;
        }

        int third = Character.digit(dni.charAt(2), 10);
        if (third < 0 || third > 5) {
            return false;
        }

        int[] coef = {2, 1, 2, 1, 2, 1, 2, 1, 2};
        int sum = 0;

        for (int i = 0; i < 9; i++) {
            int digit = Character.digit(dni.charAt(i), 10);
            int prod = coef[i] * digit;
            if (prod >= 10) {
                prod -= 9;
            }
            sum += prod;
        }

        int dv = (10 - (sum % 10)) % 10;
        int last = Character.digit(dni.charAt(9), 10);
        return dv == last;
    }
}
