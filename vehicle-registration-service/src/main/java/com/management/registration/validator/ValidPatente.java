package com.management.registration.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PatenteValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPatente {

    String message() default "Formato de patente no válido. Debe ser formato chileno (LLLL12 o LL1234)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
