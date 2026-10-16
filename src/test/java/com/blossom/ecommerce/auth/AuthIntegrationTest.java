package com.blossom.ecommerce.auth;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blossom.ecommerce.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Flujo completo de seguridad contra Postgres real (Testcontainers): registro, login, JWT,
 * rotación de refresh, logout, roles, bloqueo y CORS.
 */
@SpringBootTest(properties = "app.security.login.rate-limit-max-requests=1000")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthIntegrationTest {

	private static final String PASSWORD = "Passw0rd!";

	@Autowired
	private MockMvc mvc;

	private ResultActions post(String path, String json) throws Exception {
		return mvc.perform(MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private String credentials(String email, String password) {
		return "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password);
	}

	private String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@bloom.test";
	}

	private ResultActions register(String email) throws Exception {
		return post("/api/v1/auth/register",
				"{\"email\":\"%s\",\"password\":\"%s\",\"fullName\":\"Test User\"}".formatted(email, PASSWORD));
	}

	private String read(ResultActions result, String path) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
	}

	private String refreshBody(String token) {
		return "{\"refreshToken\":\"%s\"}".formatted(token);
	}

	private String loginAs(String email, String password, String field) throws Exception {
		return read(post("/api/v1/auth/login", credentials(email, password)).andExpect(status().isOk()), field);
	}

	@Test
	void should_register_customer_and_return_tokens_usable_on_users_me() throws Exception {
		String email = uniqueEmail();
		ResultActions registered = register(email).andExpect(status().isCreated())
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(900))
			.andExpect(jsonPath("$.user.role").value("CUSTOMER"));

		mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + read(registered, "$.accessToken")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void should_reject_register_when_email_is_taken() throws Exception {
		String email = uniqueEmail();
		register(email).andExpect(status().isCreated());

		register(email.toUpperCase()).andExpect(status().isConflict())
			.andExpect(jsonPath("$.title").value("Conflicto"));
	}

	@Test
	void should_reject_register_with_weak_password_and_invalid_email_as_problem_detail() throws Exception {
		post("/api/v1/auth/register", "{\"email\":\"no-es-correo\",\"password\":\"debil\",\"fullName\":\"X\"}")
			.andExpect(status().isBadRequest())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/problem+json")))
			.andExpect(jsonPath("$.errors", hasKey("password")))
			.andExpect(jsonPath("$.errors", hasKey("email")));
	}

	@Test
	void should_login_with_seeded_admin() throws Exception {
		post("/api/v1/auth/login", credentials("admin@bloom.test", "Admin123!")).andExpect(status().isOk())
			.andExpect(jsonPath("$.user.role").value("ADMIN"));
	}

	@Test
	void should_return_401_with_same_message_for_wrong_password_and_unknown_email() throws Exception {
		post("/api/v1/auth/login", credentials("cliente@bloom.test", "Incorrecta1!")).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Credenciales inválidas"));
		post("/api/v1/auth/login", credentials("nadie@bloom.test", "Incorrecta1!")).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Credenciales inválidas"));
	}

	@Test
	void should_return_401_problem_detail_when_token_is_missing_or_invalid() throws Exception {
		mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("application/problem+json")))
			.andExpect(jsonPath("$.status").value(401));
		mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer basura.token.invalido"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void should_enforce_roles_with_pre_authorize() throws Exception {
		String admin = "Bearer " + loginAs("admin@bloom.test", "Admin123!", "$.accessToken");
		String customer = "Bearer " + loginAs("cliente@bloom.test", "Cliente123!", "$.accessToken");

		mvc.perform(get("/api/v1/orders/me").header(HttpHeaders.AUTHORIZATION, customer)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/orders/me").header(HttpHeaders.AUTHORIZATION, admin))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(403));
		mvc.perform(MockMvcRequestBuilders.delete("/api/v1/products/99999").header(HttpHeaders.AUTHORIZATION, customer))
			.andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/orders/me")).andExpect(status().isUnauthorized());
	}

	@Test
	void should_rotate_refresh_token_and_revoke_family_when_old_one_is_reused() throws Exception {
		String email = uniqueEmail();
		String first = read(register(email), "$.refreshToken");

		ResultActions rotated = post("/api/v1/auth/refresh", refreshBody(first)).andExpect(status().isOk());
		String second = read(rotated, "$.refreshToken");

		// el token ya rotado se reutiliza: se rechaza y se revoca toda la familia
		post("/api/v1/auth/refresh", refreshBody(first)).andExpect(status().isUnauthorized());
		post("/api/v1/auth/refresh", refreshBody(second)).andExpect(status().isUnauthorized());
	}

	@Test
	void should_reject_refresh_with_unknown_token() throws Exception {
		post("/api/v1/auth/refresh", refreshBody("token-inventado")).andExpect(status().isUnauthorized());
		post("/api/v1/auth/refresh", "{}").andExpect(status().isBadRequest());
	}

	@Test
	void should_invalidate_refresh_token_on_logout() throws Exception {
		String refresh = read(register(uniqueEmail()), "$.refreshToken");

		post("/api/v1/auth/logout", refreshBody(refresh)).andExpect(status().isNoContent());

		post("/api/v1/auth/refresh", refreshBody(refresh)).andExpect(status().isUnauthorized());
		post("/api/v1/auth/logout", refreshBody("ya-no-existe")).andExpect(status().isNoContent());
	}

	@Test
	void should_lock_account_after_repeated_failures_even_with_correct_password() throws Exception {
		String email = uniqueEmail();
		register(email).andExpect(status().isCreated());

		for (int i = 0; i < 5; i++) {
			post("/api/v1/auth/login", credentials(email, "Incorrecta1!")).andExpect(status().isUnauthorized());
		}

		post("/api/v1/auth/login", credentials(email, PASSWORD)).andExpect(status().isLocked())
			.andExpect(header().exists(HttpHeaders.RETRY_AFTER))
			.andExpect(jsonPath("$.retryAfterSeconds").isNumber());
	}

	@Test
	void should_keep_catalog_swagger_and_docs_public() throws Exception {
		mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mvc.perform(get("/api/v1/products")).andExpect(status().isOk());
		mvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
	}

	@Test
	void should_answer_cors_preflight_for_allowed_origin_only() throws Exception {
		mvc.perform(options("/api/v1/auth/login").header(HttpHeaders.ORIGIN, "http://localhost:5173")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
		mvc.perform(options("/api/v1/auth/login").header(HttpHeaders.ORIGIN, "http://evil.example")
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")).andExpect(status().isForbidden());
	}

}
