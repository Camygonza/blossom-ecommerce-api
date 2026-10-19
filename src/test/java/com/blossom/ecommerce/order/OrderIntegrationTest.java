package com.blossom.ecommerce.order;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blossom.ecommerce.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class OrderIntegrationTest extends ApiIntegrationTest {

	private ResultActions order(String auth, String json) throws Exception {
		return mvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, auth)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	private int stockOf(long productId) throws Exception {
		return read(mvc.perform(get("/api/v1/products/" + productId)), "$.stock");
	}

	private String createProduct(String name, String price, int stock) throws Exception {
		ResultActions created = mvc.perform(post("/api/v1/products").header(HttpHeaders.AUTHORIZATION, adminToken())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"type\":\"PHYSICAL\",\"name\":\"%s\",\"price\":%s,\"categoryId\":1,\"stock\":%d,\"weightGrams\":100}"
				.formatted(name, price, stock)))
			.andExpect(status().isCreated());
		return String.valueOf((int) read(created, "$.id"));
	}

	@Test
	void should_create_order_computing_total_and_discounting_stock() throws Exception {
		String customer = newCustomerToken();
		String a = createProduct("Orden A", "10.50", 5);
		String b = createProduct("Orden B", "3.25", 4);

		order(customer, "{\"items\":[{\"productId\":%s,\"quantity\":2},{\"productId\":%s,\"quantity\":3}]}".formatted(a, b))
			.andExpect(status().isCreated())
			.andExpect(header().exists(HttpHeaders.LOCATION))
			.andExpect(jsonPath("$.status").value("PENDING"))
			.andExpect(jsonPath("$.total").value(30.75))
			.andExpect(jsonPath("$.items", hasSize(2)))
			.andExpect(jsonPath("$.items[0].unitPrice").value(10.5))
			.andExpect(jsonPath("$.items[0].subtotal").value(21.0));

		org.junit.jupiter.api.Assertions.assertEquals(3, stockOf(Long.parseLong(a)));
		org.junit.jupiter.api.Assertions.assertEquals(1, stockOf(Long.parseLong(b)));
	}

	@Test
	void should_merge_duplicate_lines_of_the_same_product() throws Exception {
		String a = createProduct("Orden dup", "2.00", 10);
		order(newCustomerToken(), "{\"items\":[{\"productId\":%s,\"quantity\":2},{\"productId\":%s,\"quantity\":3}]}".formatted(a, a))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].quantity").value(5))
			.andExpect(jsonPath("$.total").value(10.0));
	}

	@Test
	void should_freeze_unit_price_when_product_price_changes_later() throws Exception {
		String customer = newCustomerToken();
		String a = createProduct("Orden congelado", "20.00", 5);
		int orderId = read(order(customer, "{\"items\":[{\"productId\":%s,\"quantity\":1}]}".formatted(a)).andExpect(status().isCreated()), "$.id");

		mvc.perform(put("/api/v1/products/" + a).header(HttpHeaders.AUTHORIZATION, adminToken())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"type\":\"PHYSICAL\",\"name\":\"Orden congelado\",\"price\":99.00,\"categoryId\":1,\"stock\":4,\"weightGrams\":100}"))
			.andExpect(status().isOk());

		mvc.perform(get("/api/v1/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, customer))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].unitPrice").value(20.0))
			.andExpect(jsonPath("$.total").value(20.0));
	}

	@Test
	void should_not_oversell_and_leave_stock_untouched_when_insufficient() throws Exception {
		String a = createProduct("Orden escaso", "5.00", 2);
		String b = createProduct("Orden abundante", "5.00", 10);
		order(newCustomerToken(), "{\"items\":[{\"productId\":%s,\"quantity\":1},{\"productId\":%s,\"quantity\":3}]}".formatted(b, a))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Stock insuficiente")));
		org.junit.jupiter.api.Assertions.assertEquals(2, stockOf(Long.parseLong(a)));
		org.junit.jupiter.api.Assertions.assertEquals(10, stockOf(Long.parseLong(b)));
	}

	@Test
	void should_sell_digital_products_without_stock() throws Exception {
		order(newCustomerToken(), "{\"items\":[{\"productId\":25,\"quantity\":3}]}").andExpect(status().isCreated())
			.andExpect(jsonPath("$.items[0].productName").value("Guía de cuidado de plantas"));
	}

	@Test
	void should_validate_order_payloads() throws Exception {
		String customer = newCustomerToken();
		order(customer, "{\"items\":[]}").andExpect(status().isBadRequest());
		order(customer, "{\"items\":[{\"productId\":1,\"quantity\":0}]}").andExpect(status().isBadRequest());
		order(customer, "{\"items\":[{\"productId\":987654,\"quantity\":1}]}").andExpect(status().isBadRequest());
		order(customer, "{}").andExpect(status().isBadRequest());
	}

	@Test
	void should_restrict_orders_to_customers() throws Exception {
		order(adminToken(), "{\"items\":[{\"productId\":25,\"quantity\":1}]}").andExpect(status().isForbidden());
		mvc.perform(post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void should_list_history_newest_first_and_only_own_orders() throws Exception {
		String mine = newCustomerToken();
		String other = newCustomerToken();
		int first = read(order(mine, "{\"items\":[{\"productId\":25,\"quantity\":1}]}").andExpect(status().isCreated()), "$.id");
		int second = read(order(mine, "{\"items\":[{\"productId\":26,\"quantity\":2}]}").andExpect(status().isCreated()), "$.id");
		order(other, "{\"items\":[{\"productId\":27,\"quantity\":1}]}").andExpect(status().isCreated());

		mvc.perform(get("/api/v1/orders/me").header(HttpHeaders.AUTHORIZATION, mine))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(2))
			.andExpect(jsonPath("$.content[0].id").value(second))
			.andExpect(jsonPath("$.content[1].id").value(first))
			.andExpect(jsonPath("$.content[0].items[0].productName").exists());
		mvc.perform(get("/api/v1/orders/me").param("size", "1").header(HttpHeaders.AUTHORIZATION, mine))
			.andExpect(jsonPath("$.content", hasSize(1)))
			.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void should_hide_orders_of_other_users_with_404() throws Exception {
		String owner = newCustomerToken();
		int id = read(order(owner, "{\"items\":[{\"productId\":25,\"quantity\":1}]}").andExpect(status().isCreated()), "$.id");

		mvc.perform(get("/api/v1/orders/" + id).header(HttpHeaders.AUTHORIZATION, owner)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/orders/" + id).header(HttpHeaders.AUTHORIZATION, newCustomerToken()))
			.andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/orders/" + id)).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/orders/987654").header(HttpHeaders.AUTHORIZATION, owner)).andExpect(status().isNotFound());
	}

}
