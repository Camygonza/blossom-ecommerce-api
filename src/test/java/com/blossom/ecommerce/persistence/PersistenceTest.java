package com.blossom.ecommerce.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.blossom.ecommerce.category.domain.Category;
import com.blossom.ecommerce.category.repository.CategoryRepository;
import com.blossom.ecommerce.common.JpaAuditingConfig;
import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.OrderItem;
import com.blossom.ecommerce.order.domain.OrderStatus;
import com.blossom.ecommerce.order.repository.OrderRepository;
import com.blossom.ecommerce.product.domain.DigitalProduct;
import com.blossom.ecommerce.product.domain.PhysicalProduct;
import com.blossom.ecommerce.product.domain.Product;
import com.blossom.ecommerce.product.repository.ProductRepository;
import com.blossom.ecommerce.user.domain.Role;
import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.domain.Specification;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(JpaAuditingConfig.class)
class PersistenceTest {

	@Container
	@ServiceConnection
	static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

	@Autowired
	UserRepository users;
	@Autowired
	CategoryRepository categories;
	@Autowired
	ProductRepository products;
	@Autowired
	OrderRepository orders;
	@Autowired
	EntityManager em;

	@Test
	void should_load_seed_when_migrations_run() {
		assertThat(categories.count()).isEqualTo(5);
		assertThat(products.count()).isGreaterThanOrEqualTo(30);
		assertThat(users.findByEmail("admin@bloom.test")).get()
				.extracting(User::getRole).isEqualTo(Role.ADMIN);
		assertThat(users.findByEmail("cliente@bloom.test")).get()
				.extracting(User::getRole).isEqualTo(Role.CUSTOMER);
		assertThat(users.findByEmail("admin@bloom.test").get().getPasswordHash()).startsWith("$2a$");
	}

	@Test
	void should_resolve_subtypes_when_loading_seeded_products() {
		List<Product> all = products.findAll();
		assertThat(all).hasAtLeastOneElementOfType(PhysicalProduct.class);
		assertThat(all).hasAtLeastOneElementOfType(DigitalProduct.class);
		assertThat(all).allSatisfy(p -> assertThat(p.getImageUrl()).startsWith("https://images.unsplash.com/"));
	}

	@Test
	void should_persist_both_subtypes_when_saving_joined_inheritance() {
		Category cat = categories.save(new Category("Test", "cat de prueba"));
		PhysicalProduct physical = new PhysicalProduct();
		physical.setName("Pala");
		physical.setPrice(new BigDecimal("10.00"));
		physical.setCategory(cat);
		physical.setStock(3);
		physical.setWeightGrams(400);
		DigitalProduct digital = new DigitalProduct();
		digital.setName("Guia");
		digital.setPrice(new BigDecimal("5.00"));
		digital.setCategory(cat);
		digital.setDownloadUrl("https://downloads.bloom.test/x.pdf");
		products.saveAllAndFlush(List.of(physical, digital));
		em.clear();

		Product loadedPhysical = products.findById(physical.getId()).orElseThrow();
		Product loadedDigital = products.findById(digital.getId()).orElseThrow();
		assertThat(loadedPhysical).isInstanceOf(PhysicalProduct.class);
		assertThat(((PhysicalProduct) loadedPhysical).getStock()).isEqualTo(3);
		assertThat(loadedDigital).isInstanceOf(DigitalProduct.class);
		assertThat(((DigitalProduct) loadedDigital).getDownloadUrl()).endsWith("x.pdf");
		assertThat(loadedPhysical.getCreatedAt()).isNotNull();
		assertThat(loadedPhysical.getUpdatedAt()).isNotNull();
	}

	@Test
	void should_increment_version_when_product_is_updated() {
		PhysicalProduct p = (PhysicalProduct) products.findAll().stream()
				.filter(PhysicalProduct.class::isInstance).findFirst().orElseThrow();
		Long before = p.getVersion();
		p.setStock(p.getStock() - 1);
		products.saveAndFlush(p);
		assertThat(p.getVersion()).isEqualTo(before + 1);
	}

	@Test
	void should_persist_order_with_frozen_prices_when_saving_items() {
		User customer = users.findByEmail("cliente@bloom.test").orElseThrow();
		Product product = products.findAll().get(0);
		BigDecimal originalPrice = product.getPrice();

		Order order = new Order();
		order.setUser(customer);
		order.addItem(new OrderItem(product, 2, originalPrice));
		order.setTotal(originalPrice.multiply(BigDecimal.valueOf(2)));
		orders.saveAndFlush(order);

		product.setPrice(originalPrice.add(BigDecimal.ONE));
		products.saveAndFlush(product);
		em.clear();

		Order loaded = orders.findByUserIdOrderByCreatedAtDesc(customer.getId()).get(0);
		assertThat(loaded.getStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(loaded.getItems()).hasSize(1);
		assertThat(loaded.getItems().get(0).getUnitPrice()).isEqualByComparingTo(originalPrice);
		assertThat(loaded.getUser().getId()).isEqualTo(customer.getId());
	}

	@Test
	void should_filter_products_when_using_specifications() {
		Specification<Product> cheap = (root, q, cb) -> cb.lessThanOrEqualTo(root.get("price"), new BigDecimal("10.00"));
		Specification<Product> named = (root, q, cb) -> cb.like(cb.lower(root.get("name")), "%terracota%");

		assertThat(products.findAll(cheap.and(named))).hasSize(1);
		assertThat(products.findAll(cheap)).isNotEmpty()
				.allSatisfy(p -> assertThat(p.getPrice()).isLessThanOrEqualTo(new BigDecimal("10.00")));
	}

	@Test
	void should_reject_duplicate_email_when_saving_user() {
		assertThat(users.existsByEmail("admin@bloom.test")).isTrue();
		assertThatThrownBy(() -> users.saveAndFlush(new User("admin@bloom.test", "x", "Dup", Role.CUSTOMER)))
				.isInstanceOf(Exception.class);
	}

}
