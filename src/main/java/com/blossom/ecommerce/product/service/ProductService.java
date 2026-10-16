package com.blossom.ecommerce.product.service;

import com.blossom.ecommerce.category.domain.Category;
import com.blossom.ecommerce.category.repository.CategoryRepository;
import com.blossom.ecommerce.common.dto.PageResponse;
import com.blossom.ecommerce.common.error.BadRequestException;
import com.blossom.ecommerce.common.error.ConflictException;
import com.blossom.ecommerce.common.error.ResourceNotFoundException;
import com.blossom.ecommerce.order.repository.OrderItemRepository;
import com.blossom.ecommerce.product.domain.DigitalProduct;
import com.blossom.ecommerce.product.domain.LicenseType;
import com.blossom.ecommerce.product.domain.PhysicalProduct;
import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.domain.ProductType;
import com.blossom.ecommerce.product.dto.ProductRequest;
import com.blossom.ecommerce.product.dto.ProductResponse;
import com.blossom.ecommerce.product.mapper.ProductMapper;
import com.blossom.ecommerce.product.repository.ProductRepository;
import com.blossom.ecommerce.product.spec.ProductSpecifications;
import java.math.BigDecimal;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

	/** Propiedades por las que se permite ordenar; evita 500 por propiedades inexistentes. */
	static final Set<String> SORTABLE = Set.of("id", "name", "price", "createdAt");

	private final ProductRepository products;

	private final CategoryRepository categories;

	private final OrderItemRepository orderItems;

	private final ProductMapper mapper;

	@Transactional(readOnly = true)
	public PageResponse<ProductResponse> search(String name, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
			Pageable pageable) {
		if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
			throw new BadRequestException("minPrice no puede ser mayor que maxPrice");
		}
		pageable.getSort().forEach(order -> {
			if (!SORTABLE.contains(order.getProperty())) {
				throw new BadRequestException("No se puede ordenar por '%s'; use uno de %s".formatted(order.getProperty(),
						SORTABLE.stream().sorted().toList()));
			}
		});
		Page<ProductResponse> page = products
			.findAll(ProductSpecifications.filter(name, categoryId, minPrice, maxPrice), pageable)
			.map(mapper::toResponse);
		return PageResponse.from(page);
	}

	@Transactional(readOnly = true)
	public ProductResponse getById(Long id) {
		return mapper.toResponse(find(id));
	}

	@Transactional
	public ProductResponse create(ProductRequest request) {
		Product product = switch (request.type()) {
			case PHYSICAL -> new PhysicalProduct();
			case DIGITAL -> new DigitalProduct();
		};
		apply(product, request);
		return mapper.toResponse(products.saveAndFlush(product));
	}

	@Transactional
	public ProductResponse update(Long id, ProductRequest request) {
		Product product = find(id);
		if (typeOf(product) != request.type()) {
			throw new BadRequestException("El tipo de un producto no se puede cambiar");
		}
		apply(product, request);
		return mapper.toResponse(products.saveAndFlush(product));
	}

	@Transactional
	public void delete(Long id) {
		Product product = find(id);
		if (orderItems.existsByProductId(id)) {
			throw new ConflictException("El producto ya figura en órdenes y no se puede eliminar");
		}
		products.delete(product);
	}

	private Product find(Long id) {
		return products.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Producto %d no encontrado".formatted(id)));
	}

	private ProductType typeOf(Product product) {
		return product instanceof PhysicalProduct ? ProductType.PHYSICAL : ProductType.DIGITAL;
	}

	private void apply(Product product, ProductRequest request) {
		Category category = categories.findById(request.categoryId())
			.orElseThrow(() -> new BadRequestException("La categoría %d no existe".formatted(request.categoryId())));
		product.setName(request.name().trim());
		product.setDescription(request.description());
		product.setPrice(request.price());
		product.setImageUrl(request.imageUrl());
		product.setCategory(category);
		if (product instanceof PhysicalProduct physical) {
			if (request.stock() == null || request.weightGrams() == null) {
				throw new BadRequestException("Un producto físico requiere stock y weightGrams");
			}
			physical.setStock(request.stock());
			physical.setWeightGrams(request.weightGrams());
		}
		else if (product instanceof DigitalProduct digital) {
			if (request.downloadUrl() == null || request.downloadUrl().isBlank()) {
				throw new BadRequestException("Un producto digital requiere downloadUrl");
			}
			digital.setDownloadUrl(request.downloadUrl());
			digital.setLicenseType(request.licenseType() != null ? request.licenseType() : LicenseType.PERSONAL);
		}
	}

}
