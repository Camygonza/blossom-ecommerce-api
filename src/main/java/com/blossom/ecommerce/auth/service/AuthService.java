package com.blossom.ecommerce.auth.service;

import com.blossom.ecommerce.auth.config.AuthProperties;
import com.blossom.ecommerce.auth.dto.AuthResponse;
import com.blossom.ecommerce.auth.dto.LoginRequest;
import com.blossom.ecommerce.auth.dto.RegisterRequest;
import com.blossom.ecommerce.common.error.AccountLockedException;
import com.blossom.ecommerce.common.error.ConflictException;
import com.blossom.ecommerce.common.error.UnauthorizedException;
import com.blossom.ecommerce.user.domain.Role;
import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.mapper.UserMapper;
import com.blossom.ecommerce.user.repository.UserRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	private static final String BEARER = "Bearer";

	private final UserRepository users;

	private final PasswordEncoder encoder;

	private final JwtService jwt;

	private final RefreshTokenService refreshTokens;

	private final LoginRateLimiter rateLimiter;

	private final AuthProperties.Login loginProperties;

	private final UserMapper mapper;

	private final Clock clock;

	/** Hash descartable para igualar el tiempo de respuesta cuando el correo no existe. */
	private final String dummyHash;

	public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, RefreshTokenService refreshTokens,
			LoginRateLimiter rateLimiter, AuthProperties.Login loginProperties, UserMapper mapper, Clock clock) {
		this.users = users;
		this.encoder = encoder;
		this.jwt = jwt;
		this.refreshTokens = refreshTokens;
		this.rateLimiter = rateLimiter;
		this.loginProperties = loginProperties;
		this.mapper = mapper;
		this.clock = clock;
		this.dummyHash = encoder.encode("bloom-dummy-password");
	}

	/** El registro público solo crea clientes; el admin viene del seed. */
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalize(request.email());
		if (users.existsByEmail(email)) {
			throw new ConflictException("El correo ya está registrado");
		}
		User user = users.save(new User(email, encoder.encode(request.password()), request.fullName().trim(), Role.CUSTOMER));
		return response(user, refreshTokens.issue(user));
	}

	/**
	 * noRollbackFor: el contador de intentos fallidos debe persistir aunque se lance la excepción.
	 */
	@Transactional(noRollbackFor = UnauthorizedException.class)
	public AuthResponse login(LoginRequest request, String clientIp) {
		rateLimiter.check(clientIp);
		Instant now = clock.instant();
		User user = users.findByEmail(normalize(request.email())).orElse(null);
		if (user == null) {
			encoder.matches(request.password(), dummyHash);
			throw invalidCredentials();
		}
		if (user.getLockedUntil() != null) {
			if (user.getLockedUntil().isAfter(now)) {
				throw new AccountLockedException(Duration.between(now, user.getLockedUntil()).toSeconds() + 1);
			}
			user.setLockedUntil(null);
			user.setFailedLoginAttempts(0);
		}
		if (!encoder.matches(request.password(), user.getPasswordHash())) {
			registerFailure(user, now);
			throw invalidCredentials();
		}
		user.setFailedLoginAttempts(0);
		return response(user, refreshTokens.issue(user));
	}

	@Transactional(noRollbackFor = UnauthorizedException.class)
	public AuthResponse refresh(String refreshToken) {
		RefreshTokenService.Rotation rotation = refreshTokens.rotate(refreshToken);
		return response(rotation.user(), rotation.refreshToken());
	}

	@Transactional
	public void logout(String refreshToken) {
		refreshTokens.revoke(refreshToken);
	}

	private void registerFailure(User user, Instant now) {
		int attempts = user.getFailedLoginAttempts() + 1;
		if (attempts >= loginProperties.lockMaxFailedAttempts()) {
			user.setLockedUntil(now.plus(Duration.ofMinutes(loginProperties.lockDurationMinutes())));
			attempts = 0;
		}
		user.setFailedLoginAttempts(attempts);
	}

	private AuthResponse response(User user, String refreshToken) {
		return new AuthResponse(jwt.generateAccessToken(user), refreshToken, BEARER, jwt.accessTokenTtlSeconds(),
				mapper.toResponse(user));
	}

	private UnauthorizedException invalidCredentials() {
		return new UnauthorizedException("Credenciales inválidas");
	}

	private String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
