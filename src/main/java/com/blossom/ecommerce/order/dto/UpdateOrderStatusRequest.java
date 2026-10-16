package com.blossom.ecommerce.order.dto;

import com.blossom.ecommerce.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {

}
