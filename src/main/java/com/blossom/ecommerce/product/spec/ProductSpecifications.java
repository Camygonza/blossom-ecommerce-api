package com.blossom.ecommerce.product.spec;

import com.blossom.ecommerce.product.domain.Product;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ProductSpecifications {

	private static final char ESCAPE = '\\';

	private ProductSpecifications() {
	}

	/** Combina con AND los filtros presentes; los nulos o en blanco se ignoran. */
	public static Specification<Product> filter(String name, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
		List<Specification<Product>> parts = new ArrayList<>();
		if (name != null && !name.isBlank()) {
			parts.add(nameContains(name.trim()));
		}
		if (categoryId != null) {
			parts.add((root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId));
		}
		if (minPrice != null) {
			parts.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), minPrice));
		}
		if (maxPrice != null) {
			parts.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), maxPrice));
		}
		return Specification.allOf(parts);
	}

	private static Specification<Product> nameContains(String name) {
		String pattern = "%" + escapeLike(name.toLowerCase()) + "%";
		return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, ESCAPE);
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
