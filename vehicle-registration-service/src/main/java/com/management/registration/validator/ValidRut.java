package com.management.registration.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RutValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRut {

    String message() default "RUT inválido. Formato esperado: 12345678-9";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
