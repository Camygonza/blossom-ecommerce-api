package com.blossom.ecommerce.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blossom.ecommerce.support.ApiIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

@ExtendWith(OutputCaptureExtension.class)
class RequestIdIntegrationTest extends ApiIntegrationTest {

	@Test
	void should_generate_a_request_id_when_the_client_sends_none() throws Exception {
		String id = mvc.perform(get("/api/v1/categories"))
			.andExpect(status().isOk())
			.andExpect(header().exists("X-Request-Id"))
			.andReturn()
			.getResponse()
			.getHeader("X-Request-Id");

		assertThat(id).hasSize(36);
	}

	@Test
	void should_echo_a_safe_client_request_id_and_replace_an_unsafe_one() throws Exception {
		mvc.perform(get("/api/v1/categories").header("X-Request-Id", "front-123.abc"))
			.andExpect(header().string("X-Request-Id", "front-123.abc"));
		mvc.perform(get("/api/v1/categories").header("X-Request-Id", "mal id\nINYECTADO"))
			.andExpect(header().string("X-Request-Id", not(equalTo("mal id\nINYECTADO"))));
	}

	@Test
	void should_include_request_id_in_problem_details_from_every_error_path() throws Exception {
		// 404 (ResourceNotFoundException), 400 (validación) y 401 (filtro de seguridad)
		mvc.perform(get("/api/v1/products/987654").header("X-Request-Id", "req-404"))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.requestId").value("req-404"));
		mvc.perform(post("/api/v1/auth/login").header("X-Request-Id", "req-400")
			.contentType(MediaType.APPLICATION_JSON)
			.content("{}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.requestId").value("req-400"));
		mvc.perform(get("/api/v1/orders/me").header("X-Request-Id", "req-401"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.requestId").value("req-401"));
	}

	@Test
	void should_log_the_simulated_notification_after_commit_tagged_with_the_request_id(CapturedOutput output)
			throws Exception {
		mvc.perform(post("/api/v1/orders").header("X-Request-Id", "req-notif-1")
			.header("Authorization", newCustomerToken())
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"items\":[{\"productId\":25,\"quantity\":1}]}")).andExpect(status().isCreated());

		assertThat(output.getAll()).contains("[req-notif-1]").contains("[notificación simulada]").contains("recibida");
	}

}
