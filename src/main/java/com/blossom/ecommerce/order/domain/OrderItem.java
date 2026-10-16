package com.blossom.ecommerce.order.domain;

import com.blossom.ecommerce.common.BaseEntity;
import com.blossom.ecommerce.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "order_id")
	private Order order;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id")
	private Product product;

	@Column(nullable = false)
	private int quantity;

	/** Precio congelado al momento de la compra. */
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	public OrderItem(Product product, int quantity, BigDecimal unitPrice) {
		this.product = product;
		this.quantity = quantity;
		this.unitPrice = unitPrice;
	}

}
