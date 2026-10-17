package com.blossom.ecommerce.common;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Bloom API", version = "v1", description = "E-commerce de plantas, flores y guías digitales"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {

	private static final String PROBLEM = "Problem";

	/** Describe los errores comunes (todos ProblemDetail) en cada operación según tenga cuerpo, seguridad o id en la ruta. */
	@Bean
	OpenApiCustomizer problemResponses() {
		return openApi -> {
			Schema<?> problem = ModelConverters.getInstance().readAll(ProblemResponse.class).get(PROBLEM);
			openApi.getComponents().addSchemas(PROBLEM, problem);
			openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
				boolean writes = method != io.swagger.v3.oas.models.PathItem.HttpMethod.GET;
				boolean input = operation.getRequestBody() != null
						|| (operation.getParameters() != null && !operation.getParameters().isEmpty());
				if (input) {
					add(operation, "400", "Solicitud inválida");
				}
				if (operation.getSecurity() != null && !operation.getSecurity().isEmpty()) {
					add(operation, "401", "No autenticado");
					add(operation, "403", "Sin permiso para esta operación");
				}
				if (path.contains("{")) {
					add(operation, "404", "Recurso no encontrado");
				}
				if (writes && !path.contains("/auth/")) {
					add(operation, "409", "Conflicto (stock, estado, concurrencia o restricción)");
				}
			}));
		};
	}

	private void add(Operation operation, String code, String description) {
		if (operation.getResponses() == null) {
			operation.setResponses(new ApiResponses());
		}
		operation.getResponses()
			.addApiResponse(code, new ApiResponse().description(description)
				.content(new Content().addMediaType("application/problem+json",
						new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM)))));
	}

}
