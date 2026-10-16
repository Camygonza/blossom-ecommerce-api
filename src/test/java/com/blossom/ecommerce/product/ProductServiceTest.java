package com.blossom.ecommerce.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.blossom.ecommerce.category.domain.Category;
import com.blossom.ecommerce.category.repository.CategoryRepository;
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
import com.blossom.ecommerce.product.service.ProductService;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductServiceTest {

	@Mock
	private ProductRepository products;

	@Mock
	private CategoryRepository categories;

	@Mock
	private OrderItemRepository orderItems;

	private ProductService service;

	@BeforeEach
	void setUp() {
		ProductMapper mapper = Mappers.getMapper(ProductMapper.class);
		ReflectionTestUtils.setField(mapper, "categoryMapper", Mappers.getMapper(com.blossom.ecommerce.category.mapper.CategoryMapper.class));
		service = new ProductService(products, categories, orderItems, mapper);
		when(categories.findById(1L)).thenReturn(Optional.of(new Category("Plantas", "d")));
		when(products.saveAndFlush(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private ProductRequest physical(Integer stock, Integer weight) {
		return new ProductRequest(ProductType.PHYSICAL, "  Cactus ", "d", new BigDecimal("5.00"), null, 1L, stock, weight, null, null);
	}

	private ProductRequest digital(String url, LicenseType license) {
		return new ProductRequest(ProductType.DIGITAL, "Guía", null, new BigDecimal("5.00"), null, 1L, null, null, url, license);
	}

	@Test
	void should_create_physical_product_trimming_name() {
		ProductResponse response = service.create(physical(4, 300));

		assertThat(response.type()).isEqualTo(ProductType.PHYSICAL);
		assertThat(response.name()).isEqualTo("Cactus");
		assertThat(response.stock()).isEqualTo(4);
		assertThat(response.licenseType()).isNull();
	}

	@Test
	void should_create_digital_product_defaulting_license_to_personal() {
		ProductResponse response = service.create(digital("https://x/y.pdf", null));

		assertThat(response.type()).isEqualTo(ProductType.DIGITAL);
		assertThat(response.licenseType()).isEqualTo(LicenseType.PERSONAL);
		assertThat(response.stock()).isNull();
	}

	@Test
	void should_reject_type_specific_fields_missing() {
		assertThatThrownBy(() -> service.create(physical(null, 10))).isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> service.create(digital(" ", null))).isInstanceOf(BadRequestException.class);
	}

	@Test
	void should_reject_unknown_category() {
		when(categories.findById(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.create(physical(1, 1))).isInstanceOf(BadRequestException.class);
	}

	@Test
	void should_update_existing_product_and_forbid_type_change() {
		PhysicalProduct existing = new PhysicalProduct();
		existing.setName("Viejo");
		when(products.findById(5L)).thenReturn(Optional.of(existing));

		assertThat(service.update(5L, physical(9, 9)).stock()).isEqualTo(9);
		assertThatThrownBy(() -> service.update(5L, digital("https://x", null))).isInstanceOf(BadRequestException.class);
	}

	@Test
	void should_fail_with_not_found_for_missing_product() {
		when(products.findById(5L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getById(5L)).isInstanceOf(ResourceNotFoundException.class);
		assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void should_delete_product_only_when_not_in_orders() {
		DigitalProduct existing = new DigitalProduct();
		when(products.findById(5L)).thenReturn(Optional.of(existing));
		when(orderItems.existsByProductId(5L)).thenReturn(true);

		assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(ConflictException.class);
		verify(products, never()).delete(any(Product.class));

		when(orderItems.existsByProductId(5L)).thenReturn(false);
		service.delete(5L);
		verify(products).delete(existing);
	}

	@Test
	void should_reject_invalid_search_arguments() {
		PageRequest unsorted = PageRequest.of(0, 10);

		assertThatThrownBy(() -> service.search(null, null, BigDecimal.TEN, BigDecimal.ONE, unsorted))
			.isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> service.search(null, null, null, null, PageRequest.of(0, 10, Sort.by("category.name"))))
			.isInstanceOf(BadRequestException.class)
			.hasMessageContaining("category.name");
	}

}
