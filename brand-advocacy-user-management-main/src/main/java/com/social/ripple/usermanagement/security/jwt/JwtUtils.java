/**
 * Filename: JwtUtils.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property rights)
 * in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this software is
 * forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements explicitly
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dto.JwtTokenDetails;
import com.social.ripple.usermanagement.util.AppCache;
import com.social.ripple.usermanagement.util.AppCommonValidator;
import com.social.ripple.usermanagement.util.ConfigKeys;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JwtUtils {

	private final JwtTokenDetails jwtTokenDetails;
	private final AppCommonValidator commonValidation;
	private final AppCache configParameterCache;

	public JwtUtils(JwtTokenDetails jwtTokenDetails, AppCommonValidator commonValidation, @Lazy
	AppCache configParameterCache) {
		this.jwtTokenDetails = jwtTokenDetails;
		this.commonValidation = commonValidation;
		this.configParameterCache = configParameterCache;
	}

	public String generateAccessToken(String traceId, Authentication authentication) {
		if (authentication == null || authentication.getName() == null) {
			log.warn("[{}]|JWT|GENERATE_ACCESS_TOKEN|Authentication object is null", traceId);
			return null;
		}
		String userId = authentication.getName();
		return generateJwtToken(traceId, userId, Long.parseLong(configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_EXPIRATION_MS)),
				configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_SECRET)).getToken();
	}

	public String generateAccessToken(String traceId, String userId) {
		if (userId == null) {
			log.warn("[{}]|JWT|GENERATE_ACCESS_TOKEN|User ID is null", traceId);
			return null;
		}
		return generateJwtToken(traceId, userId, Long.parseLong(configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_EXPIRATION_MS)),
				configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_SECRET)).getToken();
	}

	public String generateRefreshToken(String traceId, String userId) {
		if (userId == null) {
			log.warn("[{}]|JWT|GENERATE_REFRESH_TOKEN|User ID is null", traceId);
			return null;
		}
		return generateJwtToken(traceId, userId,
				Long.parseLong(configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_REFRESH_TOKEN_EXPIRATION_MS)),
				configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_REFRESH_TOKEN_SECRET)).getToken();
	}

	private JwtTokenDetails generateJwtToken(String traceId, String userId, long jwtExpireTime, String jwtSecretValue) {
		try {
			Instant now = Instant.now();
			Instant expiry = now.plus(jwtExpireTime, ChronoUnit.MILLIS);

			String token = Jwts.builder()
					.setSubject(userId)
					.setIssuedAt(Date.from(now))
					.setExpiration(Date.from(expiry))
					.signWith(SignatureAlgorithm.HS512, jwtSecretValue)
					.compact();

			jwtTokenDetails.setToken(token);
			jwtTokenDetails.setExpairTime(expiry.toEpochMilli());

			log.info("[{}]|JWT|TOKEN_GENERATED|userId: {}", traceId, userId);
			return jwtTokenDetails;

		}
		catch (Exception e) {
			log.error("[{}]|JWT|TOKEN_GENERATION_FAILED|userId: {}|error: {}", traceId, userId, e.getMessage(), e);
			return null;
		}
	}

	public String getUserIdFromJwtToken(String traceId, String token) {
		try {
			Claims claims = Jwts.parser()
					.setSigningKey(configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_SECRET))
					.parseClaimsJws(token)
					.getBody();

			String userId = claims.getSubject();
			log.debug("[{}]|JWT|EXTRACT_USER_ID|Extracted userId: {}", traceId, userId);
			return userId;

		}
		catch (JwtException e) {
			log.error("[{}]|JWT|EXTRACT_USER_ID_FAILED|{}", traceId, e.getMessage(), e);
			return null;
		}
	}

	public String getUserIdFromActiveToken(String traceId, String jwt) {
		return getUserIdFromJwtToken(traceId, jwt);
	}

	public Date getTokenExpireTime(String traceId, String token) {
		try {
			Date expiration = Jwts.parser()
					.setSigningKey(configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_SECRET))
					.parseClaimsJws(token)
					.getBody()
					.getExpiration();
			log.debug("[{}]|JWT|EXTRACT_EXPIRE_TIME|{}", traceId, expiration);
			return expiration;

		}
		catch (JwtException e) {
			log.error("[{}]|JWT|EXTRACT_EXPIRE_TIME_FAILED|{}", traceId, e.getMessage(), e);
			return null;
		}
	}

	public Date getTokenIssuedTime(String traceId, String token) {
		try {
			Date issuedAt = Jwts.parser()
					.setSigningKey(configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_SECRET))
					.parseClaimsJws(token)
					.getBody()
					.getIssuedAt();
			log.debug("[{}]|JWT|EXTRACT_ISSUED_TIME|{}", traceId, issuedAt);
			return issuedAt;

		}
		catch (JwtException e) {
			log.error("[{}]|JWT|EXTRACT_ISSUED_TIME_FAILED|{}", traceId, e.getMessage(), e);
			return null;
		}
	}

	public boolean validateAccessToken(String traceId, String authToken) {
		if (commonValidation.isNullOrEmpty(traceId, authToken, "jwtToken")) {
			log.error("[{}]|JWT|VALIDATE_ACCESS_TOKEN|Invalid JWT token - Empty or null", traceId);
			return false;
		}
		return validateJwtToken(traceId, authToken, configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_SECRET));
	}

	public boolean validateRefreshToken(String traceId, String authToken) {
		if (commonValidation.isNullOrEmpty(traceId, authToken, "jwtToken")) {
			log.error("[{}]|JWT|VALIDATE_REFRESH_TOKEN|Invalid JWT token - Empty or null", traceId);
			return false;
		}
		return validateJwtToken(traceId, authToken, configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_REFRESH_TOKEN_SECRET));
	}

	private boolean validateJwtToken(String traceId, String authToken, String secret) {
		try {
			Jwts.parser().setSigningKey(secret).parseClaimsJws(authToken);
			log.debug("[{}]|JWT|VALIDATE_TOKEN|Success", traceId);
			return true;

		}
		catch (MalformedJwtException e) {
			log.warn("[{}]|JWT|VALIDATE_TOKEN_FAILED|Invalid format - {}", traceId, e.getMessage());
		}
		catch (ExpiredJwtException e) {
			log.warn("[{}]|JWT|VALIDATE_TOKEN_FAILED|Token expired - {}", traceId, e.getMessage());
		}
		catch (UnsupportedJwtException e) {
			log.warn("[{}]|JWT|VALIDATE_TOKEN_FAILED|Unsupported token - {}", traceId, e.getMessage());
		}
		catch (IllegalArgumentException e) {
			log.warn("[{}]|JWT|VALIDATE_TOKEN_FAILED|Empty claims string - {}", traceId, e.getMessage());
		}
		catch (JwtException e) {
			log.error("[{}]|JWT|VALIDATE_TOKEN_FAILED|JWT error - {}", traceId, e.getMessage(), e);
		}
		catch (Exception e) {
			log.error("[{}]|JWT|VALIDATE_TOKEN_FAILED|Unexpected error - {}", traceId, e.getMessage(), e);
		}
		return false;
	}

	public String generateResetToken(String traceId, String userId) {
		try {

			String expirationStr = configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_RESET_TOKEN_EXPIRATION_MS);

			if (expirationStr == null) {
				log.error("[{}]|JWT|GENERATE_RESET_TOKEN|Missing expiration config", traceId);
				return null;
			}

			String secret = configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_RESET_TOKEN_SECRET);
			if (secret == null) {
				log.error("[{}]|JWT|GENERATE_RESET_TOKEN|Missing secret config", traceId);
				return null;
			}
			long expiration;
			try {
				expiration = Long.parseLong(expirationStr);
			}
			catch (NumberFormatException e) {
				log.error("[{}]|JWT|GENERATE_RESET_TOKEN|Invalid expiration format: {}", traceId, expirationStr, e);
				return null;
			}

			return generateJwtToken(traceId, userId, expiration, secret).getToken();
		}
		catch (Exception e) {
			log.error("[{}]|JWT|GENERATE_RESET_TOKEN|Unexpected error: {}", traceId, e.getMessage(), e);
			return null;
		}
	}

	public boolean validateResetToken(String traceId, String authToken) {
		if (commonValidation.isNullOrEmpty(traceId, authToken, "resetToken")) {
			log.error("[{}]|JWT|VALIDATE_RESET_TOKEN|Invalid token - Empty or null", traceId);
			return false;
		}

		try {
			String secret = configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_RESET_TOKEN_SECRET);
			if (secret == null) {
				log.error("[{}]|JWT|VALIDATE_RESET_TOKEN|Missing secret configuration", traceId);
				return false;
			}

			Jwts.parser().setSigningKey(secret).parseClaimsJws(authToken);
			log.debug("[{}]|JWT|VALIDATE_RESET_TOKEN|Validation successful", traceId);
			return true;
		}
		catch (MalformedJwtException e) {
			log.warn("[{}]|JWT|VALIDATE_RESET_TOKEN|Invalid token format: {}", traceId, e.getMessage());
		}
		catch (ExpiredJwtException e) {
			log.warn("[{}]|JWT|VALIDATE_RESET_TOKEN|Token expired: {}", traceId, e.getMessage());
		}
		catch (UnsupportedJwtException e) {
			log.warn("[{}]|JWT|VALIDATE_RESET_TOKEN|Unsupported token: {}", traceId, e.getMessage());
		}
		catch (IllegalArgumentException e) {
			log.warn("[{}]|JWT|VALIDATE_RESET_TOKEN|Invalid argument: {}", traceId, e.getMessage());
		}
		catch (JwtException | NullPointerException e) {
			log.error("[{}]|JWT|VALIDATE_RESET_TOKEN|Validation failed: {}", traceId, e.getMessage(), e);
		}
		return false;
	}

	public String getSubjectFromResetToken(String traceId, String token) {
		try {
			String secret = configParameterCache.getConfigParameterValue(traceId, ConfigKeys.JWT_RESET_TOKEN_SECRET);
			if (secret == null) {
				log.error("[{}]|JWT|GET_RESET_TOKEN_SUBJECT|Missing secret configuration", traceId);
				return null;
			}

			Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();

			return claims.getSubject();
		}
		catch (JwtException | NullPointerException e) {
			log.error("[{}]|JWT|GET_RESET_TOKEN_SUBJECT|Extraction failed: {}", traceId, e.getMessage(), e);
			return null;
		}
	}
}
