package com.blossom.ecommerce.product.dto;

import com.blossom.ecommerce.category.dto.CategoryResponse;
import com.blossom.ecommerce.product.domain.LicenseType;
import com.blossom.ecommerce.product.domain.ProductType;
import java.math.BigDecimal;

/** {@code downloadUrl} no se expone en el catálogo público: es lo que se vende. */
public record ProductResponse(
		Long id,
		ProductType type,
		String name,
		String description,
		BigDecimal price,
		String imageUrl,
		CategoryResponse category,
		Integer stock,
		Integer weightGrams,
		LicenseType licenseType) {

}
