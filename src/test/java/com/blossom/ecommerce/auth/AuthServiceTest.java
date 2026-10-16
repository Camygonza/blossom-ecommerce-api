package com.blossom.ecommerce.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blossom.ecommerce.auth.config.AuthProperties;
import com.blossom.ecommerce.auth.dto.LoginRequest;
import com.blossom.ecommerce.auth.dto.RegisterRequest;
import com.blossom.ecommerce.auth.service.AuthService;
import com.blossom.ecommerce.auth.service.JwtService;
import com.blossom.ecommerce.auth.service.LoginRateLimiter;
import com.blossom.ecommerce.auth.service.RefreshTokenService;
import com.blossom.ecommerce.common.error.AccountLockedException;
import com.blossom.ecommerce.common.error.ConflictException;
import com.blossom.ecommerce.common.error.TooManyRequestsException;
import com.blossom.ecommerce.common.error.UnauthorizedException;
import com.blossom.ecommerce.user.domain.Role;
import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.dto.UserResponse;
import com.blossom.ecommerce.user.mapper.UserMapper;
import com.blossom.ecommerce.user.repository.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

	@Mock
	private UserRepository users;

	@Mock
	private PasswordEncoder encoder;

	@Mock
	private JwtService jwt;

	@Mock
	private RefreshTokenService refreshTokens;

	@Mock
	private LoginRateLimiter rateLimiter;

	@Mock
	private UserMapper mapper;

	private AuthService service;

	private User user;

	@BeforeEach
	void setUp() {
		service = new AuthService(users, encoder, jwt, refreshTokens, rateLimiter, new AuthProperties.Login(10, 60, 3, 15),
				mapper, clock);
		user = new User("ana@bloom.test", "stored-hash", "Ana", Role.CUSTOMER);
	}

	private void stubResponse() {
		when(jwt.generateAccessToken(any())).thenReturn("access");
		when(jwt.accessTokenTtlSeconds()).thenReturn(900L);
		when(mapper.toResponse(any())).thenReturn(new UserResponse(1L, "ana@bloom.test", "Ana", Role.CUSTOMER, null));
	}

	@Test
	void should_register_customer_with_normalized_email_and_hashed_password() {
		stubResponse();
		when(users.existsByEmail("ana@bloom.test")).thenReturn(false);
		when(encoder.encode("Passw0rd!")).thenReturn("encoded");
		when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
		when(refreshTokens.issue(any())).thenReturn("refresh");

		var response = service.register(new RegisterRequest("  Ana@Bloom.TEST ", "Passw0rd!", " Ana "));

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(users).save(captor.capture());
		assertThat(captor.getValue().getEmail()).isEqualTo("ana@bloom.test");
		assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded");
		assertThat(captor.getValue().getFullName()).isEqualTo("Ana");
		assertThat(captor.getValue().getRole()).isEqualTo(Role.CUSTOMER);
		assertThat(response.accessToken()).isEqualTo("access");
		assertThat(response.refreshToken()).isEqualTo("refresh");
		assertThat(response.tokenType()).isEqualTo("Bearer");
	}

	@Test
	void should_reject_register_when_email_exists() {
		when(users.existsByEmail("ana@bloom.test")).thenReturn(true);

		assertThatThrownBy(() -> service.register(new RegisterRequest("ana@bloom.test", "Passw0rd!", "Ana")))
			.isInstanceOf(ConflictException.class);
		verify(users, never()).save(any());
	}

	@Test
	void should_login_and_reset_failed_attempts_when_credentials_are_valid() {
		stubResponse();
		user.setFailedLoginAttempts(2);
		when(users.findByEmail("ana@bloom.test")).thenReturn(Optional.of(user));
		when(encoder.matches("Passw0rd!", "stored-hash")).thenReturn(true);
		when(refreshTokens.issue(user)).thenReturn("refresh");

		var response = service.login(new LoginRequest("ana@bloom.test", "Passw0rd!"), "1.1.1.1");

		assertThat(response.refreshToken()).isEqualTo("refresh");
		assertThat(user.getFailedLoginAttempts()).isZero();
		verify(rateLimiter).check("1.1.1.1");
	}

	@Test
	void should_reject_login_without_revealing_that_email_does_not_exist() {
		when(users.findByEmail("nadie@bloom.test")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.login(new LoginRequest("nadie@bloom.test", "x"), "1.1.1.1"))
			.isInstanceOf(UnauthorizedException.class)
			.hasMessage("Credenciales inválidas");
	}

	@Test
	void should_count_failures_and_lock_account_when_threshold_is_reached() {
		when(users.findByEmail("ana@bloom.test")).thenReturn(Optional.of(user));
		when(encoder.matches(any(), eq("stored-hash"))).thenReturn(false);
		var request = new LoginRequest("ana@bloom.test", "bad");

		for (int i = 0; i < 3; i++) {
			assertThatThrownBy(() -> service.login(request, "1.1.1.1")).isInstanceOf(UnauthorizedException.class);
		}

		assertThat(user.getLockedUntil()).isEqualTo(clock.instant().plus(Duration.ofMinutes(15)));
		assertThat(user.getFailedLoginAttempts()).isZero();
		assertThatThrownBy(() -> service.login(new LoginRequest("ana@bloom.test", "Passw0rd!"), "1.1.1.1"))
			.isInstanceOfSatisfying(AccountLockedException.class,
					ex -> assertThat(ex.getRetryAfterSeconds()).isEqualTo(15 * 60 + 1));
	}

	@Test
	void should_allow_login_again_when_lock_has_expired() {
		stubResponse();
		user.setLockedUntil(clock.instant().minusSeconds(1));
		when(users.findByEmail("ana@bloom.test")).thenReturn(Optional.of(user));
		when(encoder.matches("Passw0rd!", "stored-hash")).thenReturn(true);
		when(refreshTokens.issue(user)).thenReturn("refresh");

		service.login(new LoginRequest("ana@bloom.test", "Passw0rd!"), "1.1.1.1");

		assertThat(user.getLockedUntil()).isNull();
	}

	@Test
	void should_propagate_rate_limit_before_touching_the_database() {
		doThrow(new TooManyRequestsException(30)).when(rateLimiter).check("1.1.1.1");

		assertThatThrownBy(() -> service.login(new LoginRequest("ana@bloom.test", "x"), "1.1.1.1"))
			.isInstanceOf(TooManyRequestsException.class);
		verify(users, never()).findByEmail(any());
	}

	@Test
	void should_issue_new_tokens_when_refreshing() {
		stubResponse();
		when(refreshTokens.rotate("old")).thenReturn(new RefreshTokenService.Rotation(user, "new"));

		var response = service.refresh("old");

		assertThat(response.refreshToken()).isEqualTo("new");
		assertThat(response.accessToken()).isEqualTo("access");
	}

	@Test
	void should_delegate_logout_to_refresh_token_service() {
		service.logout("token");

		verify(refreshTokens).revoke("token");
	}

}
