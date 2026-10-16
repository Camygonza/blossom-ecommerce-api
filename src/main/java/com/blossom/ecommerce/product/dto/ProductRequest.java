package com.blossom.ecommerce.product.dto;

import com.blossom.ecommerce.product.domain.LicenseType;
import com.blossom.ecommerce.product.domain.ProductType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Alta/edición de producto. {@code stock} y {@code weightGrams} aplican a PHYSICAL;
 * {@code downloadUrl} y {@code licenseType} a DIGITAL (se validan en el servicio según {@code type}).
 */
public record ProductRequest(
		@NotNull ProductType type,
		@NotBlank @Size(max = 255) String name,
		@Size(max = 2000) String description,
		@NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
		@Size(max = 500) String imageUrl,
		@NotNull Long categoryId,
		@PositiveOrZero Integer stock,
		@PositiveOrZero Integer weightGrams,
		@Size(max = 500) String downloadUrl,
		LicenseType licenseType) {

}
