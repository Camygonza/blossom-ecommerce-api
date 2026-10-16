package com.blossom.ecommerce.order.web;

import com.blossom.ecommerce.auth.security.AuthenticatedUser;
import com.blossom.ecommerce.common.dto.PageResponse;
import com.blossom.ecommerce.order.dto.CreateOrderRequest;
import com.blossom.ecommerce.order.dto.OrderResponse;
import com.blossom.ecommerce.order.dto.UpdateOrderStatusRequest;
import com.blossom.ecommerce.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('CUSTOMER')")
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	@ApiResponse(responseCode = "201", description = "Creado")
	public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal AuthenticatedUser principal,
			@Valid @RequestBody CreateOrderRequest request) {
		OrderResponse created = orderService.create(principal.id(), request);
		return ResponseEntity.created(URI.create("/api/v1/orders/" + created.id())).body(created);
	}

	@GetMapping("/me")
	public PageResponse<OrderResponse> mine(@AuthenticationPrincipal AuthenticatedUser principal,
			@ParameterObject @PageableDefault(size = 10) Pageable pageable) {
		return orderService.findMine(principal.id(), pageable);
	}

	@PostMapping("/{id}/cancel")
	@Operation(summary = "Cancela una orden propia pendiente y repone el stock")
	public OrderResponse cancel(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
		return orderService.cancel(principal.id(), id);
	}

	@PatchMapping("/{id}/status")
	@PreAuthorize("hasRole('ADMIN')")
	@Operation(summary = "Cambia el estado de una orden (ADMIN)",
			description = "PENDING→PAID|CANCELLED, PAID→SHIPPED|CANCELLED, SHIPPED→DELIVERED. Cancelar repone stock.")
	public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateOrderStatusRequest request) {
		return orderService.updateStatus(id, request.status());
	}

	@GetMapping("/{id}")
	public OrderResponse get(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
		return orderService.getMine(principal.id(), id);
	}

}
