package com.blossom.ecommerce.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blossom.ecommerce.auth.config.AuthProperties;
import com.blossom.ecommerce.auth.domain.RefreshToken;
import com.blossom.ecommerce.auth.repository.RefreshTokenRepository;
import com.blossom.ecommerce.auth.service.RefreshTokenService;
import com.blossom.ecommerce.common.error.UnauthorizedException;
import com.blossom.ecommerce.user.domain.Role;
import com.blossom.ecommerce.user.domain.User;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

	@Mock
	private RefreshTokenRepository repository;

	private RefreshTokenService service;

	private final User user = new User("ana@bloom.test", "hash", "Ana", Role.CUSTOMER);

	@BeforeEach
	void setUp() {
		service = new RefreshTokenService(repository, new AuthProperties.Jwt("s".repeat(40), 900_000, 604_800_000), clock);
	}

	private RefreshToken stored(boolean revoked, Instant expiresAt) {
		RefreshToken token = new RefreshToken(user, "hash", "family-1", expiresAt);
		token.setRevoked(revoked);
		return token;
	}

	@Test
	void should_issue_token_storing_only_its_hash() {
		String raw = service.issue(user);

		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getTokenHash()).isEqualTo(RefreshTokenService.hash(raw)).isNotEqualTo(raw);
		assertThat(captor.getValue().getExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofDays(7)));
	}

	@Test
	void should_rotate_and_keep_family_when_token_is_valid() {
		RefreshToken token = stored(false, clock.instant().plusSeconds(60));
		when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

		var rotation = service.rotate("raw");

		assertThat(token.isRevoked()).isTrue();
		assertThat(rotation.user()).isSameAs(user);
		ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
		verify(repository).save(captor.capture());
		assertThat(captor.getValue().getFamilyId()).isEqualTo("family-1");
	}

	@Test
	void should_revoke_whole_family_when_a_rotated_token_is_reused() {
		when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(stored(true, clock.instant().plusSeconds(60))));

		assertThatThrownBy(() -> service.rotate("raw")).isInstanceOf(UnauthorizedException.class);

		verify(repository).revokeFamily("family-1");
		verify(repository, never()).save(any());
	}

	@Test
	void should_reject_when_token_is_unknown() {
		when(repository.findByTokenHash(anyString())).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.rotate("raw")).isInstanceOf(UnauthorizedException.class);
	}

	@Test
	void should_reject_and_revoke_when_token_is_expired() {
		RefreshToken token = stored(false, clock.instant().minusSeconds(1));
		when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

		assertThatThrownBy(() -> service.rotate("raw")).isInstanceOf(UnauthorizedException.class);

		assertThat(token.isRevoked()).isTrue();
	}

	@Test
	void should_revoke_family_on_logout_and_ignore_unknown_tokens() {
		when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(stored(false, clock.instant().plusSeconds(60))))
			.thenReturn(Optional.empty());

		service.revoke("raw");
		service.revoke("unknown");

		verify(repository).revokeFamily("family-1");
	}

}
