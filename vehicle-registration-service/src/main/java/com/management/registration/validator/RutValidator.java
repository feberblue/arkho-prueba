package com.management.registration.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RutValidator implements ConstraintValidator<ValidRut, String> {

    @Override
    public boolean isValid(String rut, ConstraintValidatorContext context) {
        if (rut == null || rut.isBlank()) {
            return false;
        }

        String rutLimpio = rut.trim().replace(".", "").replace("-", "");

        if (rutLimpio.length() < 2) {
            return false;
        }

        try {
            String cuerpo = rutLimpio.substring(0, rutLimpio.length() - 1);
            String dv = rutLimpio.substring(rutLimpio.length() - 1).toUpperCase();

            int rutNumerico = Integer.parseInt(cuerpo);
            String dvCalculado = calcularDV(rutNumerico);

            return dv.equals(dvCalculado);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Calcula el dígito verificador de un RUT chileno
     */
    private String calcularDV(int rut) {
        int suma = 0;
        int multiplo = 2;

        while (rut > 0) {
            suma += (rut % 10) * multiplo;
            rut /= 10;
            multiplo = (multiplo == 7) ? 2 : multiplo + 1;
        }

        int resto = suma % 11;
        int dv = 11 - resto;

        if (dv == 11) {
            return "0";
        } else if (dv == 10) {
            return "K";
        } else {
            return String.valueOf(dv);
        }
    }

    /**
     * Método público para normalizar RUT
     */
    public static String normalizarRut(String rut) {
        if (rut == null) {
            return null;
        }
        return rut.trim().replace(".", "").replace("-", "").toUpperCase();
    }
}
