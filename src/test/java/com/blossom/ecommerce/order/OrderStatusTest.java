package com.blossom.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.blossom.ecommerce.order.domain.OrderStatus;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrderStatusTest {

	@ParameterizedTest
	@CsvSource({ "PENDING,PAID", "PENDING,CANCELLED", "PAID,SHIPPED", "PAID,CANCELLED", "SHIPPED,DELIVERED" })
	void should_allow_forward_transitions(OrderStatus from, OrderStatus to) {
		assertThat(from.canTransitionTo(to)).isTrue();
	}

	@ParameterizedTest
	@CsvSource({ "PENDING,SHIPPED", "PENDING,DELIVERED", "PENDING,PENDING", "PAID,PENDING", "PAID,DELIVERED",
			"SHIPPED,CANCELLED", "SHIPPED,PAID", "DELIVERED,CANCELLED", "DELIVERED,PENDING", "CANCELLED,PENDING",
			"CANCELLED,PAID" })
	void should_reject_every_other_transition(OrderStatus from, OrderStatus to) {
		assertThat(from.canTransitionTo(to)).isFalse();
	}

	@Test
	void should_have_no_exit_from_final_states() {
		for (OrderStatus final_ : Set.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED)) {
			for (OrderStatus other : OrderStatus.values()) {
				assertThat(final_.canTransitionTo(other)).isFalse();
			}
		}
	}

}
