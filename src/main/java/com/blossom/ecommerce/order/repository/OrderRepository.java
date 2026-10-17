package com.blossom.ecommerce.order.repository;

import com.blossom.ecommerce.order.domain.Order;
import com.blossom.ecommerce.order.domain.OrderStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

	List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

	Page<Order> findByUserId(Long userId, Pageable pageable);

	Page<Order> findByStatus(OrderStatus status, Pageable pageable);

}
