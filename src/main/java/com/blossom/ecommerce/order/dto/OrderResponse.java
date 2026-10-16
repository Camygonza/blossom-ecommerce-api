package com.blossom.ecommerce.order.dto;

import com.blossom.ecommerce.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, OrderStatus status, BigDecimal total, Instant createdAt,
		List<OrderItemResponse> items) {

}
