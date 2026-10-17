package com.blossom.ecommerce.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/** Solo documentación: forma del ProblemDetail (RFC 7807) que devuelven todos los errores. */
@Schema(name = "Problem", description = "Error en formato RFC 7807 (application/problem+json)")
public record ProblemResponse(
		@Schema(example = "about:blank") String type,
		@Schema(example = "Recurso no encontrado") String title,
		@Schema(example = "404") int status,
		@Schema(example = "Producto 99 no encontrado") String detail,
		@Schema(example = "/api/v1/products/99") String instance,
		@Schema(description = "Identificador de la petición (cabecera X-Request-Id)") String requestId,
		@Schema(description = "Solo en 400 de validación: campo → mensaje") Map<String, String> errors) {

}
