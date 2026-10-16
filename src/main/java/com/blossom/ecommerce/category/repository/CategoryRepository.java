package com.blossom.ecommerce.category.repository;

import com.blossom.ecommerce.category.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

}
