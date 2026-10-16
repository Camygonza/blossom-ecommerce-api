package com.blossom.ecommerce.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "physical_products")
@Getter
@Setter
public class PhysicalProduct extends Product {

	@Column(nullable = false)
	private int stock;

	@Column(nullable = false)
	private int weightGrams;

}
