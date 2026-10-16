package com.blossom.ecommerce.auth.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Contraseña fuerte: 8–72 caracteres (límite de BCrypt) con mayúscula, minúscula, dígito y símbolo.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

	String message() default "La contraseña debe tener entre 8 y 72 caracteres, con mayúscula, minúscula, número y símbolo";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
