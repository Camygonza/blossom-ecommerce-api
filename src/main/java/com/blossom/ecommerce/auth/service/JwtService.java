package com.blossom.ecommerce.auth.service;

import com.blossom.ecommerce.auth.config.AuthProperties;
import com.blossom.ecommerce.auth.security.AuthenticatedUser;
import com.blossom.ecommerce.user.domain.Role;
import com.blossom.ecommerce.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Emite y valida access tokens JWT (HS256, stateless).
 */
@Service
public class JwtService {

	private static final String CLAIM_EMAIL = "email";

	private static final String CLAIM_ROLE = "role";

	private final SecretKey key;

	private final AuthProperties.Jwt properties;

	private final Clock clock;

	public JwtService(AuthProperties.Jwt properties, Clock clock) {
		byte[] secret = properties.secret().getBytes(StandardCharsets.UTF_8);
		if (secret.length < 32) {
			throw new IllegalStateException("app.jwt.secret debe tener al menos 32 bytes para HS256");
		}
		this.key = Keys.hmacShaKeyFor(secret);
		this.properties = properties;
		this.clock = clock;
	}

	public String generateAccessToken(User user) {
		Instant now = clock.instant();
		return Jwts.builder()
			.subject(String.valueOf(user.getId()))
			.claim(CLAIM_EMAIL, user.getEmail())
			.claim(CLAIM_ROLE, user.getRole().name())
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plusMillis(properties.expirationMs())))
			.signWith(key)
			.compact();
	}

	public long accessTokenTtlSeconds() {
		return properties.expirationMs() / 1000;
	}

	/**
	 * @return el usuario del token, o vacío si es inválido, está manipulado o expiró
	 */
	public Optional<AuthenticatedUser> parse(String token) {
		try {
			Claims claims = Jwts.parser()
				.verifyWith(key)
				.clock(() -> Date.from(clock.instant()))
				.build()
				.parseSignedClaims(token)
				.getPayload();
			return Optional.of(new AuthenticatedUser(Long.valueOf(claims.getSubject()), claims.get(CLAIM_EMAIL, String.class),
					Role.valueOf(claims.get(CLAIM_ROLE, String.class))));
		}
		catch (JwtException | IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

}
