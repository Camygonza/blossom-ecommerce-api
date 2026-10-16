package com.blossom.ecommerce.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Envoltorio de paginación estable para el contrato OpenAPI. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages());
	}

}
