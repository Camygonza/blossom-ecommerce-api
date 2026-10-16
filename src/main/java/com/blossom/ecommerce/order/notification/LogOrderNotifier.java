package com.blossom.ecommerce.order.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Adaptador simulado: en lugar de enviar un correo, escribe el aviso en el log (con el correo enmascarado). */
@Component
@Slf4j
public class LogOrderNotifier implements OrderNotifier {

	@Override
	public void notify(OrderEvent event) {
		if (event.previousStatus() == null) {
			log.info("[notificación simulada] a {}: orden #{} recibida, total {}", mask(event.customerEmail()),
					event.orderId(), event.total());
		}
		else {
			log.info("[notificación simulada] a {}: orden #{} pasó de {} a {}", mask(event.customerEmail()),
					event.orderId(), event.previousStatus(), event.status());
		}
	}

	public static String mask(String email) {
		if (email == null || email.indexOf('@') < 1) {
			return "***";
		}
		int at = email.indexOf('@');
		return email.charAt(0) + "***" + email.substring(at);
	}

}
