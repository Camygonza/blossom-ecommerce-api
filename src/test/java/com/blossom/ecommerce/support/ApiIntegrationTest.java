package com.blossom.ecommerce.support;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blossom.ecommerce.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Base de pruebas de integración: Postgres real (Testcontainers) y MockMvc. Todas comparten la misma
 * configuración para reutilizar el contexto y el contenedor.
 */
@SpringBootTest(properties = "app.security.login.rate-limit-max-requests=1000")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class ApiIntegrationTest {

	@Autowired
	protected MockMvc mvc;

	protected String adminToken() throws Exception {
		return "Bearer " + login("admin@bloom.test", "Admin123!");
	}

	protected String customerToken() throws Exception {
		return "Bearer " + login("cliente@bloom.test", "Cliente123!");
	}

	/** Registra un cliente nuevo y devuelve su cabecera Authorization. */
	protected String newCustomerToken() throws Exception {
		String email = "user-" + java.util.UUID.randomUUID() + "@bloom.test";
		String body = mvc
			.perform(MockMvcRequestBuilders.post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"Passw0rd!\",\"fullName\":\"Test User\"}".formatted(email)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return "Bearer " + JsonPath.<String>read(body, "$.accessToken");
	}

	protected String login(String email, String password) throws Exception {
		String body = mvc
			.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	protected <T> T read(ResultActions result, String path) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
	}

}
