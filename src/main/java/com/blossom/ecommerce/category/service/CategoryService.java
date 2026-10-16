package com.blossom.ecommerce.category.service;

import com.blossom.ecommerce.category.dto.CategoryResponse;
import com.blossom.ecommerce.category.mapper.CategoryMapper;
import com.blossom.ecommerce.category.repository.CategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

	private final CategoryRepository categories;

	private final CategoryMapper mapper;

	@Transactional(readOnly = true)
	public List<CategoryResponse> findAll() {
		return categories.findAll(Sort.by("name")).stream().map(mapper::toResponse).toList();
	}

}
