package com.blossom.ecommerce.order.notification;

/**
 * Puerto de salida para avisar al cliente de lo que ocurre con su orden. El dominio no sabe si el aviso
 * es un correo, un SMS o un webhook: hoy hay un adaptador simulado ({@link LogOrderNotifier}).
 */
public interface OrderNotifier {

	void notify(OrderEvent event);

}
