package com.blossom.ecommerce.user.service;

import com.blossom.ecommerce.common.error.ResourceNotFoundException;
import com.blossom.ecommerce.user.dto.UserResponse;
import com.blossom.ecommerce.user.mapper.UserMapper;
import com.blossom.ecommerce.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository users;

	private final UserMapper mapper;

	@Transactional(readOnly = true)
	public UserResponse getById(Long id) {
		return users.findById(id).map(mapper::toResponse).orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
	}

}
