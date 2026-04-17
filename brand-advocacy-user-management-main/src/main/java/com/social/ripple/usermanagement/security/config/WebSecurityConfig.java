/**
 * Filename: WebSecurityConfig.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
 * property rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this
 * software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements
 * explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use this software
 * internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures, routines,
 * customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted above, no
 * license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the license
 * granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any copies. This
 * software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public performance or
 * display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly prohibited and may
 * be in violation of applicable laws.
 */
package com.social.ripple.usermanagement.security.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.security.jwt.AuthTokenFilter;
import com.social.ripple.usermanagement.security.jwt.JwtUtils;
import com.social.ripple.usermanagement.util.AppCache;
import com.social.ripple.usermanagement.util.AppCommonValidator;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class WebSecurityConfig {

	private final UserDetailsService userDetailsService;

	private final AppCommonValidator commonValidation;

	private final AppCache configParameterCache;

	@Value("${app.cors.allowed-origins:http://localhost:3000}")
	private String allowedOrigins;

	@Bean
	public PasswordEncoder passwordEncoder() {
		log.info("Initializing|PasswordEncoder.");
		return new PasswordEncryptionDecryption(configParameterCache, commonValidation);
	}

	@SuppressWarnings("deprecation")
	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		try {
			log.info("Initializing|DaoAuthenticationProvider.");
			DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
			authProvider.setUserDetailsService(userDetailsService);
			authProvider.setPasswordEncoder(passwordEncoder());
			log.info("DaoAuthenticationProvider|initialized.");
			return authProvider;
		}
		catch (Exception e) {
			log.error("Failed|initialize|DaoAuthenticationProvider: {}", e.getMessage(), e);
			throw new IllegalStateException("Could not initialize authentication provider", e);
		}
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, AuthTokenFilter authTokenFilter) {
		try {
			log.info("Configuring|SecurityFilterChain.");

			http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
					.csrf(csrf -> csrf.disable())
					.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
					.exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint()).accessDeniedHandler(accessDeniedHandler()))
					.authorizeHttpRequests(
							auth -> auth
							.requestMatchers(
								    "/v1/auth/register",
								    "/v1/auth/signup",
								    "/v1/auth/invite-info",
								    "/v1/auth/login",
								    "/v1/auth/sso/register",
								    "/v1/auth/sso/signup",
								    "/v1/auth/sso/providers",
								    "/v1/auth/sso/signin",
								    "/microsoft-login.html",
								    "/recaptcha-test.html",
								    "/v1/auth/password/forgot",
								    "/v1/auth/password/reset",
								    "/api/cache/refresh",
								    "/v1/auth/token/refresh",
								    "/api/media/content/**",
									"/ws",
									"/v1/websocket/message",
									"/error"
							)
									.permitAll()
//									.requestMatchers(HttpMethod.OPTIONS, "/**")
//									.permitAll()
									.anyRequest()
									.authenticated())
					.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

			log.info("SecurityFilterChain|configured.");
			return http.build();

		}
		catch (Exception e) {
			log.error("Failed|configure|SecurityFilterChain: {}", e.getMessage(), e);
			throw new IllegalStateException("Security filter chain setup failed", e);
		}
	}

	@Bean
	public AuthTokenFilter authenticationJwtTokenFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
		try {
			log.debug("Creating|AuthTokenFilter.");
			return new AuthTokenFilter(jwtUtils, userDetailsService, commonValidation);
		}
		catch (Exception e) {
			log.error("Failed|create AuthTokenFilter: {}", e.getMessage(), e);
			throw new IllegalStateException("Could not initialize AuthTokenFilter", e);
		}
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		try {
			CorsConfiguration corsConfiguration = new CorsConfiguration();

			for (String origin : allowedOrigins.split(",")) {
				corsConfiguration.addAllowedOrigin(origin.trim());
			}

			corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
			corsConfiguration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-Trace-Id", "X-Correlation-Id", "X-Tenant-Id", "Language-Id", "Accept-Language"));
			corsConfiguration.setExposedHeaders(Arrays.asList("Authorization", "X-Trace-Id", "X-Correlation-Id"));
			corsConfiguration.setAllowCredentials(true);
			corsConfiguration.setMaxAge(3600L);
			UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
			source.registerCorsConfiguration("/**", corsConfiguration);
			return source;
		}
		catch (Exception e) {
			log.error("Failed|CORS configuration: {}", e.getMessage(), e);
			throw new IllegalStateException("Could not configure CORS", e);
		}
	}

	@Bean
	public AuthenticationEntryPoint authenticationEntryPoint() {
		return (request, response, authException) -> {
			try {

				log.warn("Unauthorized access attempt: {}", authException.getMessage());

				ErrorObj error = new ErrorObj(request.getRequestURI(), ResponseCode.USMG_401, authException.getClass().getSimpleName(),
						authException.getMessage());

				BaseResponse errorResponse = new BaseResponse(false, ResponseCode.USMG_401, "Unauthorized login attempt or token missing.",
						authException.getMessage(), new Date(), List.of(error));

				response.setContentType("application/json");
				response.setStatus(HttpStatus.UNAUTHORIZED.value());
				response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));

			}
			catch (IOException e) {
				log.error("Error writing authentication failure response: {}", e.getMessage(), e);
			}
		};
	}

	@Bean
	public AccessDeniedHandler accessDeniedHandler() {
		return (request, response, accessDeniedException) -> {
			try {
				log.warn("Access denied: {}", accessDeniedException.getMessage());

				ErrorObj error = new ErrorObj(request.getRequestURI(), ResponseCode.USMG_403, accessDeniedException.getClass().getSimpleName(),
						accessDeniedException.getMessage());

				BaseResponse errorResponse = new BaseResponse(false, ResponseCode.USMG_403, "You are not allowed to log in.",
						"Authentication attempt blocked due to role or other restriction.", new Date(), List.of(error));

				response.setContentType("application/json");
				response.setStatus(HttpStatus.FORBIDDEN.value());
				response.getWriter().write(new ObjectMapper().writeValueAsString(errorResponse));

			}
			catch (IOException e) {
				log.error("Error writing access denied response: {}", e.getMessage(), e);
			}
		};
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
		return authenticationConfiguration.getAuthenticationManager();
	}

}
