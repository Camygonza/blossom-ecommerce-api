package com.blossom.ecommerce.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.blossom.ecommerce.auth.config.AuthProperties;
import com.blossom.ecommerce.auth.service.LoginRateLimiter;
import com.blossom.ecommerce.common.error.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));

	private final LoginRateLimiter limiter = new LoginRateLimiter(new AuthProperties.Login(3, 60, 5, 15), clock);

	@Test
	void should_allow_requests_up_to_the_limit() {
		assertThatCode(() -> {
			limiter.check("1.1.1.1");
			limiter.check("1.1.1.1");
			limiter.check("1.1.1.1");
		}).doesNotThrowAnyException();
	}

	@Test
	void should_reject_when_limit_is_exceeded_and_report_retry_after() {
		for (int i = 0; i < 3; i++) {
			limiter.check("1.1.1.1");
		}
		clock.advance(Duration.ofSeconds(20));
		assertThatThrownBy(() -> limiter.check("1.1.1.1")).isInstanceOfSatisfying(TooManyRequestsException.class,
				ex -> assertThat(ex.getRetryAfterSeconds()).isEqualTo(40));
	}

	@Test
	void should_track_each_key_independently() {
		for (int i = 0; i < 3; i++) {
			limiter.check("1.1.1.1");
		}
		assertThatCode(() -> limiter.check("2.2.2.2")).doesNotThrowAnyException();
	}

	@Test
	void should_allow_again_when_window_has_passed() {
		for (int i = 0; i < 3; i++) {
			limiter.check("1.1.1.1");
		}
		clock.advance(Duration.ofSeconds(61));
		assertThatCode(() -> limiter.check("1.1.1.1")).doesNotThrowAnyException();
	}

}
