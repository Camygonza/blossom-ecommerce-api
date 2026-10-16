package com.blossom.ecommerce.auth.support;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint solo de pruebas para verificar @PreAuthorize por rol hasta que existan los de ADMIN reales. */
@RestController
@RequestMapping("/api/v1/test")
public class AdminProbeController {

	@GetMapping("/admin-only")
	@PreAuthorize("hasRole('ADMIN')")
	public String adminOnly() {
		return "ok";
	}

	@GetMapping("/customer-only")
	@PreAuthorize("hasRole('CUSTOMER')")
	public String customerOnly() {
		return "ok";
	}

}
