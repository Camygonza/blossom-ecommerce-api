package com.blossom.ecommerce.order.service;

import com.blossom.ecommerce.common.OptimisticRetry;
import com.blossom.ecommerce.common.dto.PageResponse;
import com.blossom.ecommerce.common.error.ResourceNotFoundException;
import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.OrderStatus;
import com.blossom.ecommerce.order.dto.CreateOrderRequest;
import com.blossom.ecommerce.order.dto.OrderResponse;
import com.blossom.ecommerce.order.mapper.OrderMapper;
import com.blossom.ecommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

	/** Intentos ante conflicto de bloqueo optimista; con N compradores simultáneos alcanza para que ninguno falle por azar. */
	static final int MAX_ATTEMPTS = 5;

	private final OrderRepository orders;

	private final OrderOperations operations;

	private final OrderMapper mapper;

	/** Sin {@code @Transactional}: cada intento abre su transacción en {@link OrderOperations}. */
	public OrderResponse create(Long userId, CreateOrderRequest request) {
		return OptimisticRetry.run(MAX_ATTEMPTS, () -> operations.place(userId, request));
	}

	/** El cliente cancela su propia orden pendiente; el stock vuelve al inventario. */
	public OrderResponse cancel(Long userId, Long orderId) {
		return OptimisticRetry.run(MAX_ATTEMPTS, () -> operations.transition(orderId, OrderStatus.CANCELLED, userId));
	}

	/** Cambio de estado por un administrador (pago, envío, entrega o cancelación). */
	public OrderResponse updateStatus(Long orderId, OrderStatus target) {
		return OptimisticRetry.run(MAX_ATTEMPTS, () -> operations.transition(orderId, target, null));
	}

	@Transactional(readOnly = true)
	public PageResponse<OrderResponse> findMine(Long userId, Pageable pageable) {
		Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		Page<OrderResponse> page = orders.findByUserId(userId, sorted).map(mapper::toResponse);
		return PageResponse.from(page);
	}

	/** Listado para administración: todas las órdenes (o las de un estado), las más recientes primero. */
	@Transactional(readOnly = true)
	public PageResponse<OrderResponse> findAll(OrderStatus status, Pageable pageable) {
		Pageable sorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
				Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
		Page<Order> page = status == null ? orders.findAll(sorted) : orders.findByStatus(status, sorted);
		return PageResponse.from(page.map(mapper::toResponse));
	}

	/** Una orden ajena responde 404 (igual que una inexistente) para no revelar su existencia. */
	@Transactional(readOnly = true)
	public OrderResponse getMine(Long userId, Long orderId) {
		return orders.findById(orderId)
			.filter(order -> order.getUser().getId().equals(userId))
			.map(mapper::toResponse)
			.orElseThrow(() -> new ResourceNotFoundException("Orden %d no encontrada".formatted(orderId)));
	}

}
