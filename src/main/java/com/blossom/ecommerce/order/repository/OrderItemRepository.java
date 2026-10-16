package com.blossom.ecommerce.order.repository;

import com.blossom.ecommerce.order.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

	boolean existsByProductId(Long productId);

}
