package com.blossom.ecommerce.common.error;

public class AccountLockedException extends RuntimeException {

	private final long retryAfterSeconds;

	public AccountLockedException(long retryAfterSeconds) {
		super("Cuenta bloqueada temporalmente por demasiados intentos fallidos");
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public long getRetryAfterSeconds() {
		return retryAfterSeconds;
	}

}
