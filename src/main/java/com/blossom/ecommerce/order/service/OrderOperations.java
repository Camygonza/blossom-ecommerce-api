package com.blossom.ecommerce.order.service;

import com.blossom.ecommerce.common.error.BadRequestException;
import com.blossom.ecommerce.common.error.ConflictException;
import com.blossom.ecommerce.common.error.ResourceNotFoundException;
import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.OrderItem;
import com.blossom.ecommerce.order.domain.OrderStatus;
import com.blossom.ecommerce.order.dto.CreateOrderRequest;
import com.blossom.ecommerce.order.dto.OrderItemRequest;
import com.blossom.ecommerce.order.dto.OrderResponse;
import com.blossom.ecommerce.order.mapper.OrderMapper;
import com.blossom.ecommerce.order.notification.OrderEvent;
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
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationEventPublisher;
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

	private final ApplicationEventPublisher events;

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
		Order saved = orders.saveAndFlush(order);
		events.publishEvent(eventOf(saved, null));
		return mapper.toResponse(saved);
	}

	/**
	 * Cambia el estado respetando la máquina de estados de {@link OrderStatus}. Al cancelar repone el stock de
	 * los productos físicos. Con {@code ownerId} (cliente) la orden ajena es un 404 y solo se cancela si está PENDING.
	 */
	@Transactional
	public OrderResponse transition(Long orderId, OrderStatus target, Long ownerId) {
		Order order = orders.findById(orderId)
			.filter(o -> ownerId == null || o.getUser().getId().equals(ownerId))
			.orElseThrow(() -> new ResourceNotFoundException("Orden %d no encontrada".formatted(orderId)));
		if (ownerId != null && order.getStatus() != OrderStatus.PENDING) {
			throw new ConflictException("Solo se pueden cancelar órdenes pendientes; la orden está " + order.getStatus());
		}
		if (!order.getStatus().canTransitionTo(target)) {
			throw new ConflictException("Transición no permitida: %s → %s".formatted(order.getStatus(), target));
		}
		if (target == OrderStatus.CANCELLED) {
			restock(order);
		}
		OrderStatus previous = order.getStatus();
		order.setStatus(target);
		Order saved = orders.saveAndFlush(order);
		events.publishEvent(eventOf(saved, previous));
		return mapper.toResponse(saved);
	}

	private OrderEvent eventOf(Order order, OrderStatus previous) {
		return new OrderEvent(order.getId(), order.getUser().getEmail(), previous, order.getStatus(), order.getTotal());
	}

	private void restock(Order order) {
		for (OrderItem item : order.getItems()) {
			// el producto de un OrderItem es un proxy perezoso de Product: hay que resolver la subclase real
			if (Hibernate.unproxy(item.getProduct()) instanceof PhysicalProduct physical) {
				physical.setStock(physical.getStock() + item.getQuantity());
			}
		}
	}

}
