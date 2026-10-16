package com.blossom.ecommerce.order.service;

import com.blossom.ecommerce.common.error.BadRequestException;
import com.blossom.ecommerce.common.error.ConflictException;
import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.OrderItem;
import com.blossom.ecommerce.order.dto.CreateOrderRequest;
import com.blossom.ecommerce.order.dto.OrderItemRequest;
import com.blossom.ecommerce.order.dto.OrderResponse;
import com.blossom.ecommerce.order.mapper.OrderMapper;
import com.blossom.ecommerce.order.repository.OrderRepository;
import com.blossom.ecommerce.product.domain.PhysicalProduct;
import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.repository.ProductRepository;
import com.blossom.ecommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escrituras de órdenes, una transacción por llamada. {@link OrderService} las invoca desde fuera para poder
 * reintentarlas ante un conflicto de bloqueo optimista (cada intento abre una transacción nueva).
 */
@Component
@RequiredArgsConstructor
public class OrderOperations {

	private final OrderRepository orders;

	private final ProductRepository products;

	private final UserRepository users;

	private final OrderMapper mapper;

	/**
	 * Valida productos, descuenta stock (el {@code @Version} de Product detecta escrituras concurrentes),
	 * congela el precio y calcula el total.
	 */
	@Transactional
	public OrderResponse place(Long userId, CreateOrderRequest request) {
		Map<Long, Integer> quantities = new LinkedHashMap<>();
		for (OrderItemRequest item : request.items()) {
			quantities.merge(item.productId(), item.quantity(), Integer::sum);
		}
		Map<Long, Product> found = products.findAllById(quantities.keySet())
			.stream()
			.collect(Collectors.toMap(Product::getId, Function.identity()));

		Order order = new Order();
		order.setUser(users.getReferenceById(userId));
		BigDecimal total = BigDecimal.ZERO;
		for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
			Product product = found.get(entry.getKey());
			if (product == null) {
				throw new BadRequestException("El producto %d no existe".formatted(entry.getKey()));
			}
			int quantity = entry.getValue();
			if (product instanceof PhysicalProduct physical) {
				if (physical.getStock() < quantity) {
					throw new ConflictException("Stock insuficiente para '%s': disponible %d, solicitado %d"
						.formatted(physical.getName(), physical.getStock(), quantity));
				}
				physical.setStock(physical.getStock() - quantity);
			}
			order.addItem(new OrderItem(product, quantity, product.getPrice()));
			total = total.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
		}
		order.setTotal(total);
		return mapper.toResponse(orders.saveAndFlush(order));
	}

}
