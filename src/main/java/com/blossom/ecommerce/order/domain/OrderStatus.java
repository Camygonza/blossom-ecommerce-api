package com.blossom.ecommerce.order.domain;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Ciclo de vida de una orden: PENDING → PAID → SHIPPED → DELIVERED, con cancelación posible solo mientras
 * no ha sido enviada. DELIVERED y CANCELLED son finales.
 */
public enum OrderStatus {
	PENDING, PAID, SHIPPED, DELIVERED, CANCELLED;

	private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

	static {
		ALLOWED.put(PENDING, Set.of(PAID, CANCELLED));
		ALLOWED.put(PAID, Set.of(SHIPPED, CANCELLED));
		ALLOWED.put(SHIPPED, Set.of(DELIVERED));
		ALLOWED.put(DELIVERED, Set.of());
		ALLOWED.put(CANCELLED, Set.of());
	}

	public boolean canTransitionTo(OrderStatus next) {
		return ALLOWED.get(this).contains(next);
	}

}
