package com.blossom.ecommerce.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.blossom.ecommerce.auth.domain.RefreshToken;
import com.blossom.ecommerce.auth.repository.RefreshTokenRepository;
import com.blossom.ecommerce.auth.service.RefreshTokenPurger;
import com.blossom.ecommerce.common.JpaAuditingConfig;
import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaAuditingConfig.class)
class RefreshTokenRepositoryTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Autowired
	RefreshTokenRepository tokens;

	@Autowired
	UserRepository users;

	private RefreshToken token(User user, String hash, String family, Instant expiresAt) {
		return tokens.save(new RefreshToken(user, hash, family, expiresAt));
	}

	@Test
	void should_purge_only_expired_tokens_keeping_revoked_but_valid_ones() {
		User user = users.findByEmail("cliente@bloom.test").orElseThrow();
		Instant now = Instant.now();
		token(user, "h-expired", "f1", now.minus(1, ChronoUnit.DAYS));
		RefreshToken revokedValid = token(user, "h-revoked", "f2", now.plus(1, ChronoUnit.DAYS));
		revokedValid.setRevoked(true);
		token(user, "h-valid", "f3", now.plus(1, ChronoUnit.DAYS));

		int deleted = new RefreshTokenPurger(tokens, Clock.systemUTC()).purgeExpired();

		assertThat(deleted).isEqualTo(1);
		assertThat(tokens.findByTokenHash("h-expired")).isEmpty();
		assertThat(tokens.findByTokenHash("h-revoked")).isPresent();
		assertThat(tokens.findByTokenHash("h-valid")).isPresent();
	}

	@Test
	void should_revoke_whole_family_and_count_active_ones() {
		User user = users.findByEmail("cliente@bloom.test").orElseThrow();
		Instant later = Instant.now().plus(1, ChronoUnit.DAYS);
		token(user, "a1", "fam", later);
		token(user, "a2", "fam", later);
		token(user, "b1", "other", later);

		assertThat(tokens.revokeFamily("fam")).isEqualTo(2);
		assertThat(tokens.countByFamilyIdAndRevokedFalse("fam")).isZero();
		assertThat(tokens.countByFamilyIdAndRevokedFalse("other")).isEqualTo(1);
	}

}
