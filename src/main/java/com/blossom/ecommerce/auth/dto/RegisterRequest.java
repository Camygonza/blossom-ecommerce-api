package com.blossom.ecommerce.auth.dto;

import com.blossom.ecommerce.auth.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank @Email @Size(max = 255) String email, @NotBlank @StrongPassword String password,
		@NotBlank @Size(max = 255) String fullName) {

}
