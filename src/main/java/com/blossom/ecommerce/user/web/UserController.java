package com.blossom.ecommerce.user.web;

import com.blossom.ecommerce.auth.security.AuthenticatedUser;
import com.blossom.ecommerce.user.dto.UserResponse;
import com.blossom.ecommerce.user.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
		return userService.getById(principal.id());
	}

}
