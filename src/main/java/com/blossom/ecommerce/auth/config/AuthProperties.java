package com.blossom.ecommerce.auth.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propiedades de seguridad agrupadas bajo {@code app}.
 */
public final class AuthProperties {

	private AuthProperties() {
	}

	@ConfigurationProperties("app.jwt")
	public record Jwt(String secret, long expirationMs, long refreshExpirationMs) {

	}

	@ConfigurationProperties("app.cors")
	public record Cors(List<String> allowedOrigins) {

	}

	/**
	 * Rate limit por IP y bloqueo temporal de cuenta en el login.
	 */
	@ConfigurationProperties("app.security.login")
	public record Login(int rateLimitMaxRequests, long rateLimitWindowSeconds, int lockMaxFailedAttempts,
			long lockDurationMinutes) {

	}

}
