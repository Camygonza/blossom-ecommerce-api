package com.blossom.ecommerce.auth.service;

import com.blossom.ecommerce.auth.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Borra los refresh tokens ya expirados (D11). Los revocados aún vigentes se conservan: detectan el reuso de un
 * token robado hasta que caducan.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenPurger {

	private final RefreshTokenRepository tokens;

	private final Clock clock;

	@Scheduled(cron = "${app.security.refresh-purge-cron:0 0 3 * * *}")
	@Transactional
	public int purgeExpired() {
		int deleted = tokens.deleteExpired(Instant.now(clock));
		if (deleted > 0) {
			log.info("Refresh tokens expirados eliminados: {}", deleted);
		}
		return deleted;
	}

}
