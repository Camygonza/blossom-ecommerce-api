package com.blossom.ecommerce.auth.security;

import com.blossom.ecommerce.auth.config.AuthProperties;
import com.blossom.ecommerce.auth.service.JwtService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties({ AuthProperties.Jwt.class, AuthProperties.Cors.class, AuthProperties.Login.class })
@RequiredArgsConstructor
public class SecurityConfig {

	private static final String[] PUBLIC_DOCS = { "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
			"/actuator/health/**" };

	private final JwtService jwtService;

	private final ProblemDetailSecurityHandlers handlers;

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) {
		http.csrf(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/auth/**")
				.permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**")
				.permitAll()
				.requestMatchers(PUBLIC_DOCS)
				.permitAll()
				.requestMatchers(HttpMethod.OPTIONS, "/**")
				.permitAll()
				.anyRequest()
				.authenticated())
			.exceptionHandling(e -> e.authenticationEntryPoint(handlers).accessDeniedHandler(handlers))
			.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource(AuthProperties.Cors cors) {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(cors.allowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Request-Id"));
		config.setExposedHeaders(List.of("Retry-After", "X-Request-Id"));
		config.setMaxAge(3600L);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

}
