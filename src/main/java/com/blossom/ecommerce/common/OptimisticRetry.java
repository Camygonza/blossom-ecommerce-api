package com.blossom.ecommerce.common;

import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Reintenta una operación cuando otra transacción modificó el mismo registro (bloqueo optimista).
 * La acción debe abrir su propia transacción en cada intento: así vuelve a leer el estado actual.
 */
@Slf4j
public final class OptimisticRetry {

	private OptimisticRetry() {
	}

	public static <T> T run(int maxAttempts, Supplier<T> action) {
		for (int attempt = 1;; attempt++) {
			try {
				return action.get();
			}
			catch (OptimisticLockingFailureException ex) {
				if (attempt >= maxAttempts) {
					log.warn("Conflicto de concurrencia persistente tras {} intentos", attempt);
					throw ex;
				}
				log.debug("Conflicto de concurrencia (intento {}/{}), reintentando", attempt, maxAttempts);
			}
		}
	}

}
