package com.blossom.ecommerce.order.notification;

import com.blossom.ecommerce.order.domain.OrderStatus;
import java.math.BigDecimal;

/** Hecho ocurrido en una orden. {@code previousStatus} es null cuando la orden se acaba de crear. */
public record OrderEvent(Long orderId, String customerEmail, OrderStatus previousStatus, OrderStatus status,
		BigDecimal total) {

}
