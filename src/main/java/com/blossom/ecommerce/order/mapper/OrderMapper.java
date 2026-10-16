package com.blossom.ecommerce.order.mapper;

import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.OrderItem;
import com.blossom.ecommerce.order.dto.OrderItemResponse;
import com.blossom.ecommerce.order.dto.OrderResponse;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface OrderMapper {

	OrderResponse toResponse(Order order);

	@Mapping(target = "productId", source = "product.id")
	@Mapping(target = "productName", source = "product.name")
	@Mapping(target = "subtotal", expression = "java(subtotal(item))")
	OrderItemResponse toResponse(OrderItem item);

	default BigDecimal subtotal(OrderItem item) {
		return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
	}

}
