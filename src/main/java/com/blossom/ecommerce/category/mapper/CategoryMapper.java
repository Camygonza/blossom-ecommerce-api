package com.blossom.ecommerce.category.mapper;

import com.blossom.ecommerce.category.domain.Category;
import com.blossom.ecommerce.category.dto.CategoryResponse;
import org.mapstruct.Mapper;

@Mapper
public interface CategoryMapper {

	CategoryResponse toResponse(Category category);

}
