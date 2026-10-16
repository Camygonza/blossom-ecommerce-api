package com.blossom.ecommerce.product;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

class ProductCatalogIntegrationTest extends ApiIntegrationTest {

	private static final String PHYSICAL = """
			{"type":"PHYSICAL","name":"%s","description":"d","price":%s,"categoryId":1,"stock":%d,"weightGrams":500}""";

	private static final String DIGITAL = """
			{"type":"DIGITAL","name":"%s","price":9.90,"categoryId":5,"downloadUrl":"https://downloads.bloom.test/x.pdf","licenseType":"COMMERCIAL"}""";

	private ResultActions create(String auth, String json) throws Exception {
		return mvc.perform(post("/api/v1/products").header(HttpHeaders.AUTHORIZATION, auth)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	@Test
	void should_list_products_publicly_with_pagination_metadata() throws Exception {
		mvc.perform(get("/api/v1/products").param("size", "5"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(5)))
			.andExpect(jsonPath("$.size").value(5))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(30)))
			.andExpect(jsonPath("$.content[0].category.name").exists())
			.andExpect(jsonPath("$.content[0].downloadUrl").doesNotExist());
	}

	@Test
	void should_filter_by_name_ignoring_case() throws Exception {
		mvc.perform(get("/api/v1/products").param("name", "MONSTERA"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].name", hasItem("Monstera Deliciosa")))
			.andExpect(jsonPath("$.content[*].name", everyItem(containsString("Monstera"))));
	}

	@Test
	void should_treat_like_wildcards_in_name_as_literals() throws Exception {
		mvc.perform(get("/api/v1/products").param("name", "%")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void should_filter_by_category_and_price_range_and_sort() throws Exception {
		mvc.perform(get("/api/v1/products").param("categoryId", "1")
			.param("minPrice", "25")
			.param("maxPrice", "50")
			.param("sort", "price,desc"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].category.id", everyItem(is(1))))
			.andExpect(jsonPath("$.content[*].price", everyItem(greaterThanOrEqualTo(25.0))))
			.andExpect(jsonPath("$.content[*].price", everyItem(lessThanOrEqualTo(50.0))))
			.andExpect(jsonPath("$.content[0].price").value(45.9));
	}

	@Test
	void should_reject_inverted_price_range_and_unknown_sort_with_400() throws Exception {
		mvc.perform(get("/api/v1/products").param("minPrice", "50").param("maxPrice", "10"))
			.andExpect(status().isBadRequest())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/problem+json")));
		mvc.perform(get("/api/v1/products").param("sort", "password,asc")).andExpect(status().isBadRequest());
	}

	@Test
	void should_cap_page_size() throws Exception {
		mvc.perform(get("/api/v1/products").param("size", "5000")).andExpect(status().isOk()).andExpect(jsonPath("$.size").value(50));
	}

	@Test
	void should_get_physical_and_digital_detail_and_404() throws Exception {
		mvc.perform(get("/api/v1/products/1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.type").value("PHYSICAL"))
			.andExpect(jsonPath("$.stock").exists())
			.andExpect(jsonPath("$.licenseType").doesNotExist());
		mvc.perform(get("/api/v1/products/25"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.type").value("DIGITAL"))
			.andExpect(jsonPath("$.licenseType").value("PERSONAL"))
			.andExpect(jsonPath("$.stock").doesNotExist());
		mvc.perform(get("/api/v1/products/987654")).andExpect(status().isNotFound());
	}

	@Test
	void should_list_categories_publicly() throws Exception {
		mvc.perform(get("/api/v1/categories"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(5)))
			.andExpect(jsonPath("$[*].name", hasItem("Flores")));
	}

	@Test
	void should_run_full_admin_crud() throws Exception {
		String admin = adminToken();
		ResultActions created = create(admin, PHYSICAL.formatted("Cactus CRUD", "12.50", 7)).andExpect(status().isCreated())
			.andExpect(header().exists(HttpHeaders.LOCATION))
			.andExpect(jsonPath("$.stock").value(7));
		int id = read(created, "$.id");

		mvc.perform(put("/api/v1/products/" + id).header(HttpHeaders.AUTHORIZATION, admin)
			.contentType(MediaType.APPLICATION_JSON)
			.content(PHYSICAL.formatted("Cactus CRUD 2", "15.00", 3)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Cactus CRUD 2"))
			.andExpect(jsonPath("$.price").value(15.0))
			.andExpect(jsonPath("$.stock").value(3));

		mvc.perform(delete("/api/v1/products/" + id).header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/products/" + id)).andExpect(status().isNotFound());
		mvc.perform(delete("/api/v1/products/" + id).header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isNotFound());
	}

	@Test
	void should_create_digital_product_defaulting_nothing_hidden() throws Exception {
		create(adminToken(), DIGITAL.formatted("Guía CRUD")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.type").value("DIGITAL"))
			.andExpect(jsonPath("$.licenseType").value("COMMERCIAL"))
			.andExpect(jsonPath("$.downloadUrl").doesNotExist());
	}

	@Test
	void should_validate_product_payloads() throws Exception {
		String admin = adminToken();
		create(admin, "{\"type\":\"PHYSICAL\",\"name\":\"\",\"price\":-1,\"categoryId\":1}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.name").exists())
			.andExpect(jsonPath("$.errors.price").exists());
		create(admin, "{\"type\":\"PHYSICAL\",\"name\":\"Sin stock\",\"price\":1,\"categoryId\":1}").andExpect(status().isBadRequest());
		create(admin, "{\"type\":\"DIGITAL\",\"name\":\"Sin url\",\"price\":1,\"categoryId\":5}").andExpect(status().isBadRequest());
		create(admin, PHYSICAL.formatted("Cat inexistente", "1.00", 1).replace("\"categoryId\":1", "\"categoryId\":4040"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void should_not_allow_changing_product_type() throws Exception {
		mvc.perform(put("/api/v1/products/1").header(HttpHeaders.AUTHORIZATION, adminToken())
			.contentType(MediaType.APPLICATION_JSON)
			.content(DIGITAL.formatted("Mutante")))
			.andExpect(status().isBadRequest());
	}

	@Test
	void should_forbid_writes_to_customers_and_anonymous() throws Exception {
		String customer = customerToken();
		String json = PHYSICAL.formatted("Intruso", "1.00", 1);
		create(customer, json).andExpect(status().isForbidden());
		mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isUnauthorized());
		mvc.perform(put("/api/v1/products/1").header(HttpHeaders.AUTHORIZATION, customer)
			.contentType(MediaType.APPLICATION_JSON)
			.content(json)).andExpect(status().isForbidden());
		mvc.perform(delete("/api/v1/products/1").header(HttpHeaders.AUTHORIZATION, customer)).andExpect(status().isForbidden());
		mvc.perform(delete("/api/v1/products/1")).andExpect(status().isUnauthorized());
	}

	@Test
	void should_refuse_deleting_product_that_is_in_an_order() throws Exception {
		String customer = newCustomerToken();
		ResultActions order = mvc.perform(post("/api/v1/orders").header(HttpHeaders.AUTHORIZATION, customer)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"items\":[{\"productId\":26,\"quantity\":1}]}")).andExpect(status().isCreated());
		mvc.perform(delete("/api/v1/products/26").header(HttpHeaders.AUTHORIZATION, adminToken()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409));
		org.junit.jupiter.api.Assertions.assertNotNull(read(order, "$.id"));
	}

}
