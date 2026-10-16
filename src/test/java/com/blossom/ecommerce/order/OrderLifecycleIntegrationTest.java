package com.blossom.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blossom.ecommerce.common.error.ConflictException;
import com.blossom.ecommerce.order.domain.OrderStatus;
import com.blossom.ecommerce.order.service.OrderService;
import com.blossom.ecommerce.support.ApiIntegrationTest;
import com.blossom.ecommerce.user.repository.UserRepository;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class OrderLifecycleIntegrationTest extends ApiIntegrationTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private UserRepository users;

	private long product(int stock) throws Exception {
		ResultActions created = mvc.perform(post("/api/v1/products").header(HttpHeaders.AUTHORIZATION, adminToken())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"type\":\"PHYSICAL\",\"name\":\"Ciclo %d\",\"price\":8.00,\"categoryId\":1,\"stock\":%d,\"weightGrams\":10}"
				.formatted(System.nanoTime(), stock)))
			.andExpect(status().isCreated());
		return ((Number) read(created, "$.id")).longValue();
	}

	private long buy(String customer, long productId, int quantity) throws Exception {
		ResultActions created = mvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, customer)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"items\":[{\"productId\":%d,\"quantity\":%d}]}".formatted(productId, quantity)))
			.andExpect(status().isCreated());
		return ((Number) read(created, "$.id")).longValue();
	}

	private int stockOf(long productId) throws Exception {
		return read(mvc.perform(get("/api/v1/products/" + productId)), "$.stock");
	}

	private ResultActions setStatus(long orderId, String status) throws Exception {
		return mvc.perform(patch("/api/v1/orders/" + orderId + "/status").header(HttpHeaders.AUTHORIZATION, adminToken())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"%s\"}".formatted(status)));
	}

	@Test
	void should_walk_the_happy_path_as_admin() throws Exception {
		long order = buy(newCustomerToken(), product(5), 1);

		setStatus(order, "PAID").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
		setStatus(order, "SHIPPED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHIPPED"));
		setStatus(order, "DELIVERED").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"));
		setStatus(order, "CANCELLED").andExpect(status().isConflict());
	}

	@Test
	void should_reject_skipping_states_and_unknown_values() throws Exception {
		long order = buy(newCustomerToken(), product(5), 1);

		setStatus(order, "SHIPPED").andExpect(status().isConflict()).andExpect(jsonPath("$.detail").exists());
		setStatus(order, "PENDING").andExpect(status().isConflict());
		setStatus(order, "NOPE").andExpect(status().isBadRequest());
		setStatus(987654, "PAID").andExpect(status().isNotFound());
	}

	@Test
	void should_restock_when_customer_cancels_a_pending_order() throws Exception {
		String customer = newCustomerToken();
		long product = product(10);
		long order = buy(customer, product, 4);
		assertThat(stockOf(product)).isEqualTo(6);

		mvc.perform(post("/api/v1/orders/" + order + "/cancel").header(HttpHeaders.AUTHORIZATION, customer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("CANCELLED"));

		assertThat(stockOf(product)).isEqualTo(10);
		mvc.perform(post("/api/v1/orders/" + order + "/cancel").header(HttpHeaders.AUTHORIZATION, customer))
			.andExpect(status().isConflict());
		assertThat(stockOf(product)).isEqualTo(10);
	}

	@Test
	void should_restock_when_admin_cancels_a_paid_order() throws Exception {
		long product = product(10);
		long order = buy(newCustomerToken(), product, 3);
		setStatus(order, "PAID").andExpect(status().isOk());

		setStatus(order, "CANCELLED").andExpect(status().isOk());

		assertThat(stockOf(product)).isEqualTo(10);
	}

	@Test
	void should_not_let_customers_cancel_paid_or_foreign_orders() throws Exception {
		String owner = newCustomerToken();
		long order = buy(owner, product(5), 1);

		mvc.perform(post("/api/v1/orders/" + order + "/cancel").header(HttpHeaders.AUTHORIZATION, newCustomerToken()))
			.andExpect(status().isNotFound());
		setStatus(order, "PAID").andExpect(status().isOk());
		mvc.perform(post("/api/v1/orders/" + order + "/cancel").header(HttpHeaders.AUTHORIZATION, owner))
			.andExpect(status().isConflict());
	}

	@Test
	void should_restrict_status_changes_to_admin() throws Exception {
		long order = buy(newCustomerToken(), product(5), 1);

		mvc.perform(patch("/api/v1/orders/" + order + "/status").header(HttpHeaders.AUTHORIZATION, customerToken())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"PAID\"}")).andExpect(status().isForbidden());
		mvc.perform(patch("/api/v1/orders/" + order + "/status").contentType(MediaType.APPLICATION_JSON)
			.content("{\"status\":\"PAID\"}")).andExpect(status().isUnauthorized());
		mvc.perform(post("/api/v1/orders/" + order + "/cancel").header(HttpHeaders.AUTHORIZATION, adminToken()))
			.andExpect(status().isForbidden());
	}

	@Test
	void should_restock_only_once_when_the_same_order_is_cancelled_concurrently() throws Exception {
		long product = product(10);
		long order = buy(customerToken(), product, 4);
		Long ownerId = users.findByEmail("cliente@bloom.test").orElseThrow().getId();

		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch go = new CountDownLatch(1);
		Callable<Boolean> cancel = () -> {
			go.await();
			try {
				orderService.cancel(ownerId, order);
				return true;
			}
			catch (ConflictException alreadyCancelled) {
				return false;
			}
		};
		Future<Boolean> first = pool.submit(cancel);
		Future<Boolean> second = pool.submit(cancel);
		go.countDown();
		int succeeded = (first.get() ? 1 : 0) + (second.get() ? 1 : 0);
		pool.shutdown();

		assertThat(succeeded).isEqualTo(1);
		assertThat(stockOf(product)).isEqualTo(10);
		assertThat(orderService.getMine(ownerId, order).status()).isEqualTo(OrderStatus.CANCELLED);
	}
}
