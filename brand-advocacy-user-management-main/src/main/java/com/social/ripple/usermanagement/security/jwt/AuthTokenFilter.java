/**
 * Filename: AuthTokenFilter.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
 * rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this software
 * is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements explicitly
 * covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use this software
 * internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures, routines,
 * customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted above, no
 * license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the license
 * granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any copies. This
 * software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public performance or
 * display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly prohibited and may
 * be in violation of applicable laws.
 */
package com.social.ripple.usermanagement.security.jwt;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.social.ripple.usermanagement.util.AppCommonValidator;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuthTokenFilter extends OncePerRequestFilter {

	private final JwtUtils jwtUtil;
	private final UserDetailsService userDetailsService;
	private final AppCommonValidator commonValidation;

	public AuthTokenFilter(JwtUtils jwtUtil, UserDetailsService userDetailsService, AppCommonValidator commonValidation) {
		this.jwtUtil = jwtUtil;
		this.userDetailsService = userDetailsService;
		this.commonValidation = commonValidation;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String traceId = commonValidation.getOrGenerateRequestId(request.getHeader("x-trace-id"));

		try {
			if (SecurityContextHolder.getContext().getAuthentication() != null) {
				log.info("[{}]|AUTH|FILTER|Skipped - SecurityContext already contains authentication", traceId);
				filterChain.doFilter(request, response);
				return;
			}

			String jwt = parseJwt(request, traceId);
			if (jwt == null) {
				log.warn("[{}]|JWT|PARSE|Missing - No token found in request", traceId);
				filterChain.doFilter(request, response);
				return;
			}

			if (!jwtUtil.validateAccessToken(traceId, jwt)) {
				log.warn("[{}]|JWT|VALIDATE|Failed - Invalid or expired token", traceId);
				response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired token");
				return;
			}

			log.debug("[{}]|JWT|VALIDATE|Success - Valid token: {}", traceId, jwt);

			String userId = jwtUtil.getUserIdFromActiveToken(traceId, jwt);
			if (!StringUtils.hasText(userId)) {
				log.warn("[{}]|JWT|PARSE|Failed - Extracted userId is null or empty", traceId);
				response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid token - userId is empty");
				return;
			}

			log.debug("[{}]|JWT|PARSE|Extracted userId: {}", traceId, userId);

			UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

			if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
				UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
						userDetails.getAuthorities());
				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);

				log.info("[{}]|AUTH|SUCCESS|User authenticated - userId: {}", traceId, userId);
			}
			else {
				log.warn("[{}]|AUTH|FAILURE|User account is disabled or locked - userId: {}", traceId, userId);
				response.sendError(HttpStatus.UNAUTHORIZED.value(), "User account is disabled or locked");
				return;
			}

		}
		catch (UsernameNotFoundException ex) {
			log.warn("[{}]|AUTH|ERROR|User not found - {}", traceId, ex.getMessage());
			response.sendError(HttpStatus.UNAUTHORIZED.value(), "User not found");
			return;

		}
		catch (JwtException ex) {
			log.warn("[{}]|JWT|ERROR|{}", traceId, ex.getMessage());
			response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid JWT");
			return;

		}
		catch (Exception e) {

			log.error("[{}]|AUTH|ERROR|Unexpected error occurred", traceId, e);
			response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Unexpected error occurred");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private String parseJwt(HttpServletRequest request, String traceId) {
		String headerAuth = request.getHeader("Authorization");

		if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
			log.debug("[{}]|JWT|PARSE|Authorization header found", traceId);
			return headerAuth.substring(7);
		}

		log.warn("[{}]|JWT|PARSE|Authorization header is missing or malformed", traceId);
		return null;
	}
}