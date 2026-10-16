package com.blossom.ecommerce.category.web;

import com.blossom.ecommerce.category.dto.CategoryResponse;
import com.blossom.ecommerce.category.service.CategoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories")
public class CategoryController {

	private final CategoryService categoryService;

	@GetMapping
	public List<CategoryResponse> list() {
		return categoryService.findAll();
	}

}
