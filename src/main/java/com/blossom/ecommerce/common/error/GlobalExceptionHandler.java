package com.blossom.ecommerce.common.error;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce excepciones propias y de Spring MVC (404, 405, 415, JSON mal formado...) a respuestas ProblemDetail (RFC 7807).
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		Map<String, String> errors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos");
		pd.setTitle("Validación fallida");
		pd.setProperty("errors", errors);
		return ResponseEntity.badRequest().body(pd);
	}

	@ExceptionHandler({ UnauthorizedException.class, AuthenticationException.class })
	ResponseEntity<ProblemDetail> handleUnauthorized(RuntimeException ex) {
		String detail = ex instanceof UnauthorizedException ? ex.getMessage() : "Autenticación requerida";
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, detail);
		pd.setTitle("No autenticado");
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").body(pd);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail handleAccessDenied(AccessDeniedException ex) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "No tienes permiso para esta operación");
		pd.setTitle("Acceso denegado");
		return pd;
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	ProblemDetail handleNotFound(ResourceNotFoundException ex) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		pd.setTitle("Recurso no encontrado");
		return pd;
	}

	@ExceptionHandler({ ConflictException.class, DataIntegrityViolationException.class })
	ProblemDetail handleConflict(RuntimeException ex) {
		String detail = ex instanceof ConflictException ? ex.getMessage() : "La operación viola una restricción de datos";
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
		pd.setTitle("Conflicto");
		return pd;
	}

	@ExceptionHandler(AccountLockedException.class)
	ResponseEntity<ProblemDetail> handleLocked(AccountLockedException ex) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.LOCKED, ex.getMessage());
		pd.setTitle("Cuenta bloqueada");
		pd.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
		return ResponseEntity.status(HttpStatus.LOCKED)
			.header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
			.body(pd);
	}

	@ExceptionHandler(TooManyRequestsException.class)
	ResponseEntity<ProblemDetail> handleTooMany(TooManyRequestsException ex) {
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
		pd.setTitle("Límite de solicitudes excedido");
		pd.setProperty("retryAfterSeconds", ex.getRetryAfterSeconds());
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
			.header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
			.body(pd);
	}

	@ExceptionHandler(Exception.class)
	ProblemDetail handleUnexpected(Exception ex) {
		log.error("Error no controlado", ex);
		ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor");
		pd.setTitle("Error interno");
		return pd;
	}

}
