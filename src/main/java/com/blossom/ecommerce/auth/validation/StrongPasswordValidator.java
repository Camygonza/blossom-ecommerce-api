package com.blossom.ecommerce.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) {
			return true; // la obligatoriedad la valida @NotBlank
		}
		if (value.length() < 8 || value.length() > 72) {
			return false;
		}
		boolean upper = false;
		boolean lower = false;
		boolean digit = false;
		boolean symbol = false;
		for (char c : value.toCharArray()) {
			if (Character.isUpperCase(c)) {
				upper = true;
			}
			else if (Character.isLowerCase(c)) {
				lower = true;
			}
			else if (Character.isDigit(c)) {
				digit = true;
			}
			else if (!Character.isWhitespace(c)) {
				symbol = true;
			}
		}
		return upper && lower && digit && symbol;
	}

}
