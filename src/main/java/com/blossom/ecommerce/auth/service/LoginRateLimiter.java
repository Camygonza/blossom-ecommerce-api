package com.blossom.ecommerce.auth.service;

import com.blossom.ecommerce.auth.config.AuthProperties;
import com.blossom.ecommerce.common.error.TooManyRequestsException;
import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Rate limit de ventana deslizante en memoria, por clave (IP). Suficiente para una instancia;
 * con varias réplicas habría que moverlo a Redis (ver decisiones.md).
 */
@Component
public class LoginRateLimiter {

	private static final int PURGE_THRESHOLD = 10_000;

	private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

	private final AuthProperties.Login properties;

	private final Clock clock;

	public LoginRateLimiter(AuthProperties.Login properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Registra un intento y lanza {@link TooManyRequestsException} si la clave supera el límite.
	 */
	public void check(String key) {
		long now = clock.millis();
		long windowMs = properties.rateLimitWindowSeconds() * 1000;
		if (hits.size() > PURGE_THRESHOLD) {
			hits.values().removeIf(d -> isStale(d, now, windowMs));
		}
		Deque<Long> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
		synchronized (deque) {
			while (!deque.isEmpty() && now - deque.peekFirst() >= windowMs) {
				deque.pollFirst();
			}
			if (deque.size() >= properties.rateLimitMaxRequests()) {
				long retryMs = windowMs - (now - deque.peekFirst());
				throw new TooManyRequestsException(Math.max(1, (retryMs + 999) / 1000));
			}
			deque.addLast(now);
		}
	}

	private boolean isStale(Deque<Long> deque, long now, long windowMs) {
		synchronized (deque) {
			return deque.isEmpty() || now - deque.peekLast() >= windowMs;
		}
	}

}
