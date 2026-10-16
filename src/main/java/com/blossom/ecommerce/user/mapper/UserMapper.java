package com.blossom.ecommerce.user.mapper;

import com.blossom.ecommerce.user.domain.User;
import com.blossom.ecommerce.user.dto.UserResponse;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

	UserResponse toResponse(User user);

}
