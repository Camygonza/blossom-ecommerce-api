package com.blossom.ecommerce.auth.security;

import com.blossom.ecommerce.common.web.RequestIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * 401 y 403 generados por los filtros de Spring Security, en formato ProblemDetail
 * (los {@code @RestControllerAdvice} no ven estas excepciones).
 */
@Component
@RequiredArgsConstructor
public class ProblemDetailSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final JsonMapper jsonMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
			throws IOException {
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		write(response, HttpStatus.UNAUTHORIZED, "No autenticado", "Autenticación requerida");
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
			throws IOException {
		write(response, HttpStatus.FORBIDDEN, "Acceso denegado", "No tienes permiso para esta operación");
	}

	private void write(HttpServletResponse response, HttpStatus status, String title, String detail) throws IOException {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
		pd.setTitle(title);
		String requestId = MDC.get(RequestIdFilter.MDC_KEY);
		if (requestId != null) {
			pd.setProperty("requestId", requestId);
		}
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(jsonMapper.writeValueAsString(pd));
	}

}
