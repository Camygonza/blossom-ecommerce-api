package com.blossom.ecommerce.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;

class OptimisticRetryTest {

	@Test
	void should_retry_until_the_action_succeeds() {
		AtomicInteger calls = new AtomicInteger();

		String result = OptimisticRetry.run(5, () -> {
			if (calls.incrementAndGet() < 3) {
				throw new OptimisticLockingFailureException("conflicto");
			}
			return "ok";
		});

		assertThat(result).isEqualTo("ok");
		assertThat(calls).hasValue(3);
	}

	@Test
	void should_rethrow_after_max_attempts() {
		AtomicInteger calls = new AtomicInteger();

		assertThatThrownBy(() -> OptimisticRetry.run(3, () -> {
			calls.incrementAndGet();
			throw new OptimisticLockingFailureException("conflicto");
		})).isInstanceOf(OptimisticLockingFailureException.class);
		assertThat(calls).hasValue(3);
	}

	@Test
	void should_not_retry_other_exceptions() {
		AtomicInteger calls = new AtomicInteger();

		assertThatThrownBy(() -> OptimisticRetry.run(3, () -> {
			calls.incrementAndGet();
			throw new IllegalStateException("otro");
		})).isInstanceOf(IllegalStateException.class);
		assertThat(calls).hasValue(1);
	}

}
