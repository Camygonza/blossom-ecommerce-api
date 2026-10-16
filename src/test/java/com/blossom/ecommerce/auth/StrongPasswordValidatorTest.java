package com.blossom.ecommerce.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.blossom.ecommerce.auth.validation.StrongPasswordValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class StrongPasswordValidatorTest {

	private final StrongPasswordValidator validator = new StrongPasswordValidator();

	@ParameterizedTest
	@ValueSource(strings = { "Admin123!", "Cliente123!", "aB3$aB3$", "Una frase Larga 9?" })
	void should_accept_when_password_is_strong(String password) {
		assertThat(validator.isValid(password, null)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "Sh0rt!A", "alllowercase1!", "ALLUPPERCASE1!", "NoDigitsHere!", "NoSymbol1234A", "Ab1!" })
	void should_reject_when_password_is_weak(String password) {
		assertThat(validator.isValid(password, null)).isFalse();
	}

	@Test
	void should_reject_when_password_exceeds_bcrypt_limit() {
		assertThat(validator.isValid("Aa1!" + "x".repeat(70), null)).isFalse();
	}

	@Test
	void should_accept_null_so_not_blank_reports_it() {
		assertThat(validator.isValid(null, null)).isTrue();
	}

}
