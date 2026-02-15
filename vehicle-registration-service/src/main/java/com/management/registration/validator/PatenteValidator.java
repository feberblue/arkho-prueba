package com.management.registration.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PatenteValidator implements ConstraintValidator<ValidPatente, String> {

    // Patente antigua Chile: AA1234 (2 letras + 4 números)
    // Patente nueva Chile: BBBB12 (4 letras + 2 números)
    private static final Pattern PATENTE_ANTIGUA = Pattern.compile("^[A-Z]{2}[0-9]{4}$");
    private static final Pattern PATENTE_NUEVA = Pattern.compile("^[A-Z]{4}[0-9]{2}$");

    @Override
    public void initialize(ValidPatente constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String patente, ConstraintValidatorContext context) {
        if (patente == null || patente.isBlank()) {
            return false;
        }

        String patenteNormalizada = patente.trim().toUpperCase().replace("-", "");

        return PATENTE_ANTIGUA.matcher(patenteNormalizada).matches() ||
                PATENTE_NUEVA.matcher(patenteNormalizada).matches();
    }

    public static String normalizarPatente(String patente) {
        if (patente == null) {
            return null;
        }
        return patente.trim().toUpperCase().replace("-", "").replace(" ", "");
    }
}
