package com.blossom.ecommerce.user.dto;

import com.blossom.ecommerce.user.domain.Role;
import java.time.Instant;

public record UserResponse(Long id, String email, String fullName, Role role, Instant createdAt) {

}
