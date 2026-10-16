package com.blossom.ecommerce.common.error;

import com.blossom.ecommerce.common.web.RequestIdFilter;
import org.slf4j.MDC;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/** Añade {@code requestId} a todo ProblemDetail para que soporte pueda localizar la petición en los logs. */
@RestControllerAdvice
public class RequestIdProblemDetailAdvice implements ResponseBodyAdvice<Object> {

	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return true;
	}

	@Override
	public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
			Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
			ServerHttpResponse response) {
		if (body instanceof ProblemDetail problem) {
			String requestId = MDC.get(RequestIdFilter.MDC_KEY);
			if (requestId != null) {
				problem.setProperty("requestId", requestId);
			}
		}
		return body;
	}

}
