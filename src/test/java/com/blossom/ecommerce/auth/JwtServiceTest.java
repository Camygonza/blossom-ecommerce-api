package com.blossom.ecommerce.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.blossom.ecommerce.auth.config.AuthProperties;
import com.blossom.ecommerce.auth.service.JwtService;
import com.blossom.ecommerce.user.domain.Role;
import com.blossom.ecommerce.user.domain.User;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

	private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";

	private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

	private final JwtService jwt = new JwtService(new AuthProperties.Jwt(SECRET, 900_000, 604_800_000), clock);

	private User user() {
		User user = new User("ana@bloom.test", "hash", "Ana", Role.ADMIN);
		ReflectionTestUtils.setField(user, "id", 7L);
		return user;
	}

	@Test
	void should_roundtrip_user_data_when_token_is_valid() {
		var parsed = jwt.parse(jwt.generateAccessToken(user()));

		assertThat(parsed).hasValueSatisfying(u -> {
			assertThat(u.id()).isEqualTo(7L);
			assertThat(u.email()).isEqualTo("ana@bloom.test");
			assertThat(u.role()).isEqualTo(Role.ADMIN);
		});
		assertThat(jwt.accessTokenTtlSeconds()).isEqualTo(900);
	}

	@Test
	void should_reject_when_token_is_expired() {
		String token = jwt.generateAccessToken(user());
		clock.advance(Duration.ofMinutes(16));

		assertThat(jwt.parse(token)).isEmpty();
	}

	@Test
	void should_reject_when_token_is_tampered_or_garbage() {
		String token = jwt.generateAccessToken(user());

		assertThat(jwt.parse(token.substring(0, token.length() - 3) + "abc")).isEmpty();
		assertThat(jwt.parse("not-a-jwt")).isEmpty();
	}

	@Test
	void should_reject_when_signed_with_another_secret() {
		var other = new JwtService(new AuthProperties.Jwt("another-secret-another-secret-0123456789", 900_000, 1), clock);

		assertThat(jwt.parse(other.generateAccessToken(user()))).isEmpty();
	}

	@Test
	void should_fail_to_start_when_secret_is_too_short() {
		assertThatThrownBy(() -> new JwtService(new AuthProperties.Jwt("short", 1, 1), clock))
			.isInstanceOf(IllegalStateException.class);
	}

}
