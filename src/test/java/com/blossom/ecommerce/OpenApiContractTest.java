package com.blossom.ecommerce;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.blossom.ecommerce.support.ApiIntegrationTest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contrato OpenAPI fijado en {@code docs/openapi.json}: el front se alinea con ese archivo y el build falla si la API
 * cambia sin actualizarlo. Para regenerarlo: {@code ./mvnw test -Dtest=OpenApiContractTest -Dopenapi.update=true}.
 */
class OpenApiContractTest extends ApiIntegrationTest {

	private static final Path CONTRACT = Path.of("docs", "openapi.json");

	@Test
	void should_match_the_committed_openapi_contract() throws Exception {
		String actual = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		if (Boolean.getBoolean("openapi.update")) {
			Files.createDirectories(CONTRACT.getParent());
			JsonMapper mapper = JsonMapper.builder().build();
			Files.writeString(CONTRACT, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(actual)) + "\n",
					StandardCharsets.UTF_8);
			return;
		}
		String expected = Files.readString(CONTRACT, StandardCharsets.UTF_8);
		JSONAssert.assertEquals(expected, actual, JSONCompareMode.STRICT);
	}

}
