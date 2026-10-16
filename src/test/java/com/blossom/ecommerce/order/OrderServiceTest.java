package com.blossom.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blossom.ecommerce.common.error.BadRequestException;
import com.blossom.ecommerce.common.error.ConflictException;
import com.blossom.ecommerce.common.error.ResourceNotFoundException;
import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.OrderStatus;
import com.blossom.ecommerce.order.dto.CreateOrderRequest;
import com.blossom.ecommerce.order.dto.OrderItemRequest;
import com.blossom.ecommerce.order.dto.OrderResponse;
import com.blossom.ecommerce.order.mapper.OrderMapper;
import com.blossom.ecommerce.order.repository.OrderRepository;
import com.blossom.ecommerce.order.service.OrderOperations;
import com.blossom.ecommerce.order.service.OrderService;
import com.blossom.ecommerce.product.domain.DigitalProduct;
import com.blossom.ecommerce.product.domain.PhysicalProduct;
import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.repository.ProductRepository;
import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

	@Mock
	private OrderRepository orders;

	@Mock
	private ProductRepository products;

	@Mock
	private UserRepository users;

	private OrderService service;

	@BeforeEach
	void setUp() {
		OrderMapper mapper = Mappers.getMapper(OrderMapper.class);
		service = new OrderService(orders, new OrderOperations(orders, products, users, mapper), mapper);
		when(users.getReferenceById(7L)).thenReturn(user(7L));
		when(orders.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private User user(Long id) {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", id);
		return user;
	}

	private PhysicalProduct physical(Long id, String price, int stock) {
		PhysicalProduct p = new PhysicalProduct();
		ReflectionTestUtils.setField(p, "id", id);
		p.setName("P" + id);
		p.setPrice(new BigDecimal(price));
		p.setStock(stock);
		return p;
	}

	private DigitalProduct digital(Long id, String price) {
		DigitalProduct p = new DigitalProduct();
		ReflectionTestUtils.setField(p, "id", id);
		p.setName("D" + id);
		p.setPrice(new BigDecimal(price));
		return p;
	}

	private CreateOrderRequest request(OrderItemRequest... items) {
		return new CreateOrderRequest(List.of(items));
	}

	@Test
	void should_compute_total_freeze_price_and_discount_stock_when_creating_order() {
		PhysicalProduct a = physical(1L, "10.50", 5);
		DigitalProduct d = digital(2L, "9.90");
		when(products.findAllById(any())).thenReturn(List.of(a, d));

		OrderResponse response = service.create(7L, request(new OrderItemRequest(1L, 2), new OrderItemRequest(2L, 1)));

		assertThat(response.total()).isEqualByComparingTo("30.90");
		assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
		assertThat(response.items()).hasSize(2);
		assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo("10.50");
		assertThat(a.getStock()).isEqualTo(3);
	}

	@Test
	void should_merge_repeated_product_lines() {
		PhysicalProduct a = physical(1L, "2.00", 10);
		when(products.findAllById(any())).thenReturn(List.of(a));

		OrderResponse response = service.create(7L, request(new OrderItemRequest(1L, 2), new OrderItemRequest(1L, 3)));

		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).quantity()).isEqualTo(5);
		assertThat(a.getStock()).isEqualTo(5);
	}

	@Test
	void should_reject_with_conflict_when_stock_is_insufficient() {
		when(products.findAllById(any())).thenReturn(List.of(physical(1L, "2.00", 1)));

		assertThatThrownBy(() -> service.create(7L, request(new OrderItemRequest(1L, 2))))
			.isInstanceOf(ConflictException.class)
			.hasMessageContaining("Stock insuficiente");
		verify(orders, never()).saveAndFlush(any());
	}

	@Test
	void should_reject_unknown_product() {
		when(products.findAllById(any())).thenReturn(List.of());

		assertThatThrownBy(() -> service.create(7L, request(new OrderItemRequest(99L, 1))))
			.isInstanceOf(BadRequestException.class);
	}

	@Test
	void should_persist_order_with_user_reference() {
		when(products.findAllById(any())).thenReturn(List.<Product>of(digital(2L, "1.00")));

		service.create(7L, request(new OrderItemRequest(2L, 1)));

		ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
		verify(orders).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getUser().getId()).isEqualTo(7L);
		assertThat(captor.getValue().getItems()).hasSize(1);
	}

	@Test
	void should_return_own_order_and_hide_foreign_one() {
		Order order = new Order();
		order.setUser(user(7L));
		when(orders.findById(1L)).thenReturn(Optional.of(order));
		when(orders.findById(2L)).thenReturn(Optional.empty());

		assertThat(service.getMine(7L, 1L).status()).isEqualTo(OrderStatus.PENDING);
		assertThatThrownBy(() -> service.getMine(8L, 1L)).isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> service.getMine(7L, 2L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void should_page_history_sorted_by_newest_regardless_of_requested_sort() {
		ArgumentCaptor<org.springframework.data.domain.Pageable> captor = ArgumentCaptor
			.forClass(org.springframework.data.domain.Pageable.class);
		when(orders.findByUserId(any(), any())).thenReturn(new PageImpl<>(List.of()));

		service.findMine(7L, PageRequest.of(2, 5, org.springframework.data.domain.Sort.by("total")));

		verify(orders).findByUserId(any(), captor.capture());
		assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
		assertThat(captor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
		assertThat(captor.getValue().getSort().getOrderFor("total")).isNull();
	}

}
