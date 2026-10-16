package com.blossom.ecommerce.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "digital_products")
@Getter
@Setter
public class DigitalProduct extends Product {

	@Column(nullable = false, length = 500)
	private String downloadUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LicenseType licenseType = LicenseType.PERSONAL;

}
