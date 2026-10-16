package com.blossom.ecommerce.auth.security;

import com.blossom.ecommerce.user.domain.Role;

/**
 * Principal que el filtro JWT deja en el SecurityContext (sin consultar la BD).
 */
public record AuthenticatedUser(Long id, String email, Role role) {

}
