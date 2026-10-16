package com.blossom.ecommerce.order.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifica solo cuando la transacción de la orden ya se confirmó (un intento revertido por concurrencia no
 * avisa) y un fallo del notificador nunca deshace ni retrasa la orden.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationListener {

	private final OrderNotifier notifier;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onOrderEvent(OrderEvent event) {
		try {
			notifier.notify(event);
		}
		catch (RuntimeException ex) {
			log.error("No se pudo notificar la orden {}", event.orderId(), ex);
		}
	}

}
