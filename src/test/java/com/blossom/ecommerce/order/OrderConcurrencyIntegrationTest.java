package com.blossom.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.blossom.ecommerce.common.error.ConflictException;
import com.blossom.ecommerce.order.dto.CreateOrderRequest;
import com.blossom.ecommerce.order.dto.OrderItemRequest;
import com.blossom.ecommerce.order.service.OrderService;
import com.blossom.ecommerce.product.domain.PhysicalProduct;
import com.blossom.ecommerce.product.repository.ProductRepository;
import com.blossom.ecommerce.support.ApiIntegrationTest;
import com.blossom.ecommerce.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Compradores simultáneos contra Postgres real: el stock nunca se sobrevende. */
class OrderConcurrencyIntegrationTest extends ApiIntegrationTest {

	@Autowired
	private OrderService orderService;

	@Autowired
	private ProductRepository products;

	@Autowired
	private UserRepository users;

	private Long createProduct(int stock) throws Exception {
		String json = "{\"type\":\"PHYSICAL\",\"name\":\"Concurrencia %d\",\"price\":4.00,\"categoryId\":1,\"stock\":%d,\"weightGrams\":10}"
			.formatted(System.nanoTime(), stock);
		String body = mvc
			.perform(MockMvcRequestBuilders.post("/api/v1/products")
				.header("Authorization", adminToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
			.andReturn()
			.getResponse()
			.getContentAsString();
		return ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();
	}

	private int stockOf(Long productId) {
		return ((PhysicalProduct) products.findById(productId).orElseThrow()).getStock();
	}

	/** Lanza {@code buyers} compras de 1 unidad a la vez y devuelve cuántas se crearon. */
	private int buyConcurrently(Long productId, int buyers) throws Exception {
		Long userId = users.findByEmail("cliente@bloom.test").orElseThrow().getId();
		CreateOrderRequest request = new CreateOrderRequest(List.of(new OrderItemRequest(productId, 1)));
		ExecutorService pool = Executors.newFixedThreadPool(buyers);
		CountDownLatch ready = new CountDownLatch(buyers);
		CountDownLatch go = new CountDownLatch(1);
		AtomicInteger created = new AtomicInteger();
		List<Future<?>> futures = new ArrayList<>();
		for (int i = 0; i < buyers; i++) {
			Callable<Void> buyer = () -> {
				ready.countDown();
				go.await();
				try {
					orderService.create(userId, request);
					created.incrementAndGet();
				}
				catch (ConflictException soldOut) {
					// sin stock: resultado válido, lo que no puede ocurrir es vender de más
				}
				return null;
			};
			futures.add(pool.submit(buyer));
		}
		ready.await();
		go.countDown();
		for (Future<?> f : futures) {
			f.get();
		}
		pool.shutdown();
		return created.get();
	}

	@Test
	void should_sell_exactly_the_available_stock_when_buyers_exceed_it() throws Exception {
		Long productId = createProduct(3);

		int created = buyConcurrently(productId, 5);

		assertThat(created).isEqualTo(3);
		assertThat(stockOf(productId)).isZero();
	}

	@Test
	void should_keep_stock_consistent_with_orders_created_under_contention() throws Exception {
		Long productId = createProduct(6);

		int created = buyConcurrently(productId, 5);

		assertThat(created).isEqualTo(5);
		assertThat(stockOf(productId)).isEqualTo(1);
	}

}
