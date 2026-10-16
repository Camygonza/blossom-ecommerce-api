package com.blossom.ecommerce.product.mapper;

import com.blossom.ecommerce.category.mapper.CategoryMapper;
import com.blossom.ecommerce.product.domain.DigitalProduct;
import com.blossom.ecommerce.product.domain.PhysicalProduct;
import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.dto.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = CategoryMapper.class)
public interface ProductMapper {

	default ProductResponse toResponse(Product product) {
		if (product instanceof PhysicalProduct physical) {
			return fromPhysical(physical);
		}
		return fromDigital((DigitalProduct) product);
	}

	@Mapping(target = "type", constant = "PHYSICAL")
	@Mapping(target = "licenseType", ignore = true)
	ProductResponse fromPhysical(PhysicalProduct product);

	@Mapping(target = "type", constant = "DIGITAL")
	@Mapping(target = "stock", ignore = true)
	@Mapping(target = "weightGrams", ignore = true)
	ProductResponse fromDigital(DigitalProduct product);

}
