package com.blossom.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.OrderStatus;
import com.blossom.ecommerce.order.dto.CreateOrderRequest;
import com.blossom.ecommerce.order.dto.OrderItemRequest;
import com.blossom.ecommerce.order.mapper.OrderMapper;
import com.blossom.ecommerce.order.notification.LogOrderNotifier;
import com.blossom.ecommerce.order.notification.OrderEvent;
import com.blossom.ecommerce.order.notification.OrderNotificationListener;
import com.blossom.ecommerce.order.notification.OrderNotifier;
import com.blossom.ecommerce.order.repository.OrderRepository;
import com.blossom.ecommerce.order.service.OrderOperations;
import com.blossom.ecommerce.product.domain.DigitalProduct;
import com.blossom.ecommerce.product.repository.ProductRepository;
import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(OutputCaptureExtension.class)
class OrderNotificationTest {

	private final OrderRepository orders = mock(OrderRepository.class);

	private final ProductRepository products = mock(ProductRepository.class);

	private final UserRepository users = mock(UserRepository.class);

	private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);

	private final OrderOperations operations = new OrderOperations(orders, products, users,
			Mappers.getMapper(OrderMapper.class), events);

	private User customer() {
		User user = new User();
		ReflectionTestUtils.setField(user, "id", 7L);
		user.setEmail("cliente@bloom.test");
		return user;
	}

	@Test
	void should_publish_event_without_previous_status_when_order_is_placed() {
		DigitalProduct guide = new DigitalProduct();
		ReflectionTestUtils.setField(guide, "id", 2L);
		guide.setName("Guía");
		guide.setPrice(new BigDecimal("9.90"));
		when(users.getReferenceById(7L)).thenReturn(customer());
		when(products.findAllById(any())).thenReturn(List.of(guide));
		when(orders.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

		operations.place(7L, new CreateOrderRequest(List.of(new OrderItemRequest(2L, 2))));

		ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
		verify(events).publishEvent(captor.capture());
		assertThat(captor.getValue().previousStatus()).isNull();
		assertThat(captor.getValue().status()).isEqualTo(OrderStatus.PENDING);
		assertThat(captor.getValue().total()).isEqualByComparingTo("19.80");
		assertThat(captor.getValue().customerEmail()).isEqualTo("cliente@bloom.test");
	}

	@Test
	void should_publish_event_with_previous_status_when_state_changes() {
		Order order = new Order();
		order.setUser(customer());
		when(orders.findById(1L)).thenReturn(Optional.of(order));
		when(orders.saveAndFlush(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

		operations.transition(1L, OrderStatus.PAID, null);

		ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
		verify(events).publishEvent(captor.capture());
		assertThat(captor.getValue().previousStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(captor.getValue().status()).isEqualTo(OrderStatus.PAID);
	}

	@Test
	void should_log_masked_email_in_the_simulated_adapter(CapturedOutput output) {
		LogOrderNotifier notifier = new LogOrderNotifier();

		notifier.notify(new OrderEvent(5L, "cliente@bloom.test", null, OrderStatus.PENDING, new BigDecimal("19.80")));
		notifier.notify(new OrderEvent(5L, "cliente@bloom.test", OrderStatus.PAID, OrderStatus.SHIPPED, BigDecimal.TEN));

		assertThat(output).contains("[notificación simulada]", "c***@bloom.test", "orden #5 recibida")
			.contains("pasó de PAID a SHIPPED")
			.doesNotContain("cliente@bloom.test");
	}

	@Test
	void should_mask_missing_or_malformed_emails() {
		assertThat(LogOrderNotifier.mask(null)).isEqualTo("***");
		assertThat(LogOrderNotifier.mask("sin-arroba")).isEqualTo("***");
	}

	@Test
	void should_never_propagate_notifier_failures() {
		OrderNotifier failing = mock(OrderNotifier.class);
		OrderEvent event = new OrderEvent(1L, "a@b.c", null, OrderStatus.PENDING, BigDecimal.ONE);
		doThrow(new IllegalStateException("smtp caído")).when(failing).notify(event);

		assertThatCode(() -> new OrderNotificationListener(failing).onOrderEvent(event)).doesNotThrowAnyException();
	}

}
