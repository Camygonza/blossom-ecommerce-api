package com.blossom.ecommerce.auth.service;

import com.blossom.ecommerce.auth.config.AuthProperties;
import com.blossom.ecommerce.auth.domain.RefreshToken;
import com.blossom.ecommerce.auth.repository.RefreshTokenRepository;
import com.blossom.ecommerce.common.error.UnauthorizedException;
import com.blossom.ecommerce.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens opacos con rotación. En BD solo se guarda el hash SHA-256 del token.
 * <p>
 * Cada /refresh revoca el token usado y emite otro de la misma familia. Si llega un token ya
 * revocado (robo o replay) se revoca toda la familia, así el atacante y el usuario legítimo
 * pierden la sesión y deben autenticarse de nuevo.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final SecureRandom RANDOM = new SecureRandom();

	private final RefreshTokenRepository repository;

	private final AuthProperties.Jwt properties;

	private final Clock clock;

	/** Resultado de una rotación: el usuario dueño y el nuevo token en claro. */
	public record Rotation(User user, String refreshToken) {

	}

	@Transactional
	public String issue(User user) {
		return create(user, UUID.randomUUID().toString());
	}

	@Transactional(noRollbackFor = UnauthorizedException.class)
	public Rotation rotate(String rawToken) {
		RefreshToken token = repository.findByTokenHash(hash(rawToken))
			.orElseThrow(() -> new UnauthorizedException("Refresh token inválido"));
		if (token.isRevoked()) {
			repository.revokeFamily(token.getFamilyId());
			throw new UnauthorizedException("Refresh token reutilizado: sesión revocada");
		}
		if (token.getExpiresAt().isBefore(clock.instant())) {
			token.setRevoked(true);
			throw new UnauthorizedException("Refresh token expirado");
		}
		token.setRevoked(true);
		return new Rotation(token.getUser(), create(token.getUser(), token.getFamilyId()));
	}

	/** Cierra la sesión: revoca la familia del token. Idempotente y silencioso si el token no existe. */
	@Transactional
	public void revoke(String rawToken) {
		repository.findByTokenHash(hash(rawToken)).ifPresent(t -> repository.revokeFamily(t.getFamilyId()));
	}

	private String create(User user, String familyId) {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		repository.save(new RefreshToken(user, hash(raw), familyId, clock.instant().plusMillis(properties.refreshExpirationMs())));
		return raw;
	}

	public static String hash(String raw) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
