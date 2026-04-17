/**
 * Filename: SsoTokenValidatorServiceImpl.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all
 * intellectual property rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix.
 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the
 * license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies. This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not
 * publicly available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public
 * performance or display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly
 * prohibited and may be in violation of applicable laws.
 */
package com.social.ripple.usermanagement.service.implementation;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.social.ripple.usermanagement.dto.GoogleUserDto;
import com.social.ripple.usermanagement.dto.MicrosoftUserDto;
import com.social.ripple.usermanagement.service.ISsoTokenValidatorService;
import com.social.ripple.usermanagement.util.AppCache;
import com.social.ripple.usermanagement.util.ConfigKeys;
import com.social.ripple.usermanagement.util.constants.ApplicationConstants;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@DependsOn("initialConfigurationLoader")
public class SsoTokenValidatorServiceImpl implements ISsoTokenValidatorService {

	private final GoogleIdTokenVerifier verifier;
	private final ObjectMapper objectMapper;

	public SsoTokenValidatorServiceImpl(ObjectMapper objectMapper) {
		try {
			this.verifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
					.setAudience(Collections.singletonList(AppCache.configParameters.get(ConfigKeys.GOOGLE_SSO_CLIENT_ID).getConfigValue()))
					.build();
			this.objectMapper = objectMapper;
		}
		catch (GeneralSecurityException | IOException e) {
			throw new IllegalStateException("Failed to initialize GoogleIdTokenVerifier", e);
		}
	}

	@Override
	public GoogleUserDto validateGoogleSsoToken(String traceId, String idTokenString) {
		try {
			log.info("[{}]|SSO|TokenValidation|Start", traceId);
			log.debug("[{}]|SSO|TokenValidation|Input received: {}", traceId, idTokenString != null ? "TOKEN_PRESENT" : "TOKEN_NULL");
			GoogleIdToken idToken = verifier.verify(idTokenString);
			if (idToken == null) {
				log.warn("[{}]|SSO|TokenValidation|Missing or empty ID token", traceId);
				return createInvalidUserDto();
			}
			GoogleIdToken.Payload payload = extractPayloadFromToken(traceId, idTokenString);
			GoogleUserDto ssoUser = buildUserDtoFromPayload(traceId, payload);

			log.debug(
					"[{}]|SSO|TokenValidation|User Info - Email: {}, Name: {}, PictureURL: {}, Locale: {}, EmailVerified: {}, GivenName: {}, FamilyName: {}",
					traceId, ssoUser.getEmail(), ssoUser.getName(), ssoUser.getPictureUrl(), ssoUser.getLocale(), ssoUser.isEmailVerified(),
					ssoUser.getGivenName(), ssoUser.getFamilyName());
			log.info("[{}]|SSO|TokenValidation|End", traceId);

			return ssoUser;
		}
		catch (Exception e) {
			log.error("[{}]|SSO|TokenValidation|Token verification failed|message : {}", traceId, e.getMessage(), e);
			return createInvalidUserDto();
		}
	}

	private GoogleIdToken.Payload extractPayloadFromToken(String traceId, String idTokenString) {
		try {
			GoogleIdToken idToken = verifier.verify(idTokenString);
			if (idToken != null) {
				return idToken.getPayload();
			}
			else {
				log.warn("[{}]|SSO|TokenValidation|Token verification failed", traceId);
				return null;
			}
		}
		catch (GeneralSecurityException | java.io.IOException e) {
			log.error("[{}]|SSO|TokenValidation|Exception occurred during token verification: {}", traceId, e.getMessage(), e);
		}
		return null;
	}

	private GoogleUserDto buildUserDtoFromPayload(String traceId, GoogleIdToken.Payload payload) {
		GoogleUserDto userDto = new GoogleUserDto();

		if (payload == null) {
			log.warn("[{}]|SSO|TokenValidation|Cannot build user info. Payload is null", traceId);
			userDto.setEmailVerified(false);
			return userDto;
		}

		userDto.setEmail(payload.getEmail());
		userDto.setName((String) payload.get(ApplicationConstants.SSO_PAYLOAD_NAME));
		userDto.setPictureUrl((String) payload.get(ApplicationConstants.SSO_PAYLOAD_PICTURE));
		userDto.setLocale((String) payload.get(ApplicationConstants.SSO_PAYLOAD_LOCALE));
		userDto.setEmailVerified(Boolean.TRUE.equals(payload.getEmailVerified()));
		userDto.setGivenName((String) payload.get(ApplicationConstants.SSO_PAYLOAD_GIVEN_NAME));
		userDto.setFamilyName((String) payload.get(ApplicationConstants.SSO_PAYLOAD_FAMILY_NAME));

		log.debug("[{}]|SSO|TokenValidation|Successfully extracted user info for email: {}", traceId, userDto.getEmail());
		return userDto;
	}

	private GoogleUserDto createInvalidUserDto() {
		GoogleUserDto userDto = new GoogleUserDto();
		userDto.setEmailVerified(false);
		return userDto;
	}

	@Override
	public MicrosoftUserDto validateMicrosoftSsoToken(String traceId, String idToken) {
		log.info("[{}]|SSO|TokenValidation|Start token validation", traceId);
		MicrosoftUserDto responseDto = new MicrosoftUserDto();

		try {
			validateTokenPresence(traceId, idToken);
			String[] tokenParts = parseTokenParts(traceId, idToken);

			Map<String, Object> header = extractTokenHeader(traceId, tokenParts[0]);
			PublicKey publicKey = fetchMicrosoftPublicKey(traceId, (String) header.get("kid"));

			verifyTokenSignature(traceId, tokenParts, publicKey);
			Claims claims = extractAndValidateClaims(traceId, tokenParts);

			responseDto = mapToMicrosoftUserDto(traceId, claims);
			responseDto.setEmailVerified(true);

			log.info("[{}]|SSO|TokenValidation|Token validation successful", traceId);
			return responseDto;

		}
		catch (IllegalArgumentException e) {
			log.warn("[{}]|SSO|TokenValidation|Invalid token format: {}", traceId, e.getMessage());
			responseDto.setEmailVerified(false);
			return responseDto;
		}
		catch (SecurityException e) {
			log.warn("[{}]|SSO|TokenValidation|Token validation failed: {}", traceId, e.getMessage());
			responseDto.setEmailVerified(false);
			return responseDto;
		}
		catch (Exception e) {
			log.error("[{}]|SSO|TokenValidation|Unexpected error during token validation", traceId, e);
			responseDto.setEmailVerified(false);
			return responseDto;
		}
	}

	private void validateTokenPresence(String traceId, String idToken) {
		if (idToken == null || idToken.trim().isEmpty()) {
			log.error("[{}]|SSO|TokenValidation|Empty or null token provided", traceId);
			throw new IllegalArgumentException("ID token is null or empty");
		}
	}

	private String[] parseTokenParts(String traceId, String idToken) {
		String[] parts = idToken.split("\\.");
		if (parts.length != 3) {
			log.error("[{}]|SSO|TokenValidation|Invalid JWT format - expected 3 parts, got {}", traceId, parts.length);
			throw new IllegalArgumentException("Invalid JWT format");
		}
		return parts;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> extractTokenHeader(String traceId, String headerPart) {
		try {
			String headerJson = new String(Base64.getUrlDecoder().decode(headerPart));
			return objectMapper.readValue(headerJson, Map.class);
		}
		catch (Exception e) {
			log.error("[{}]|SSO|TokenValidation|Failed to decode token header", traceId, e);
			throw new IllegalArgumentException("Invalid token header", e);
		}
	}

	@SuppressWarnings("unchecked")
	private PublicKey fetchMicrosoftPublicKey(String traceId, String kid) throws Exception {
		log.debug("[{}]|SSO|TokenValidation|Fetching public key for kid: {}", traceId, kid);

		try (InputStream input = new URL(AppCache.configParameters.get(ConfigKeys.AZURE_AD_JWKS_KEYS_ENDPOINT).getConfigValue()).openStream()) {
			Map<String, Object> jwks = objectMapper.readValue(input, Map.class);
			List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");

			for (Map<String, Object> key : keys) {
				if (kid.equals(key.get("kid"))) {
					log.debug("[{}]|SSO|TokenValidation|Found matching public key", traceId);
					return createPublicKeyFromJwk(key);
				}
			}

			log.error("[{}]|SSO|TokenValidation|No matching public key found for kid: {}", traceId, kid);
			throw new IllegalStateException("No matching public key found for kid: " + kid);
		}
	}

	private PublicKey createPublicKeyFromJwk(Map<String, Object> key) throws Exception {
		String n = (String) key.get("n");
		String e = (String) key.get("e");

		BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
		BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));

		RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
		return KeyFactory.getInstance("RSA").generatePublic(spec);
	}

	private void verifyTokenSignature(String traceId, String[] tokenParts, PublicKey publicKey) throws Exception {
		log.debug("[{}]|SSO|TokenValidation|Verifying token signature", traceId);

		byte[] signedContent = (tokenParts[0] + "." + tokenParts[1]).getBytes("UTF-8");
		byte[] signatureBytes = Base64.getUrlDecoder().decode(tokenParts[2]);

		Signature sig = Signature.getInstance("SHA256withRSA");
		sig.initVerify(publicKey);
		sig.update(signedContent);

		if (!sig.verify(signatureBytes)) {
			log.error("[{}]|SSO|TokenValidation|Token signature verification failed", traceId);
			throw new SecurityException("Invalid token signature");
		}
	}

	private Claims extractAndValidateClaims(String traceId, String[] tokenParts) {
		try {
			Claims claims = Jwts.parser().parseClaimsJwt(tokenParts[0] + "." + tokenParts[1] + ".").getBody();

			log.debug("[{}]|SSO|TokenValidation|Extracted claims: {}", traceId, claims);
			validateStandardClaims(traceId, claims);

			return claims;
		}
		catch (Exception e) {
			log.error("[{}]|SSO|TokenValidation|Failed to parse or validate claims", traceId, e);
			throw new SecurityException("Invalid token claims", e);
		}
	}

	private void validateStandardClaims(String traceId, Claims claims) {
		validateIssuer(traceId, claims);
		validateAudience(traceId, claims);
		validateExpiration(traceId, claims);
	}

	private void validateIssuer(String traceId, Claims claims) {
		if (!AppCache.configParameters.get(ConfigKeys.AZURE_AD_ISSUER).getConfigValue().equals(claims.getIssuer())) {
			log.error("[{}]|SSO|TokenValidation|Invalid issuer. Expected: {}, Actual: {}", traceId,
					AppCache.configParameters.get(ConfigKeys.AZURE_AD_ISSUER).getConfigValue(), claims.getIssuer());
			throw new SecurityException("Invalid token issuer");
		}
	}

	private void validateAudience(String traceId, Claims claims) {
		Object audClaim = claims.get("aud");
		boolean audienceMatch = false;

		if (audClaim instanceof String) {
			audienceMatch = AppCache.configParameters.get(ConfigKeys.AZURE_AD_CLIENT_ID).getConfigValue().equals(audClaim);
		}
		else if (audClaim instanceof List) {
			audienceMatch = ((List<?>) audClaim).contains(AppCache.configParameters.get(ConfigKeys.AZURE_AD_CLIENT_ID).getConfigValue());
		}

		if (!audienceMatch) {
			log.error("[{}]|SSO|TokenValidation|Invalid audience. Expected: {}, Actual: {}", traceId,
					AppCache.configParameters.get(ConfigKeys.AZURE_AD_CLIENT_ID).getConfigValue(), audClaim);
			throw new SecurityException("Invalid token audience");
		}
	}

	private void validateExpiration(String traceId, Claims claims) {
		Date expiration = claims.getExpiration();
		if (expiration == null) {
			log.error("[{}]|SSO|TokenValidation|Missing expiration claim", traceId);
			throw new SecurityException("Token has no expiration");
		}

		if (expiration.before(new Date())) {
			log.error("[{}]|SSO|TokenValidation|Token expired at {}", traceId, expiration);
			throw new SecurityException("Token has expired");
		}
	}

	private MicrosoftUserDto mapToMicrosoftUserDto(String traceId, Claims claims) {
		MicrosoftUserDto dto = new MicrosoftUserDto();
		dto.setUserId(claims.getSubject());
		dto.setEmail(claims.get("email", String.class));
		dto.setName(claims.get("name", String.class));
		dto.setPreferredUsername(claims.get("preferred_username", String.class));
		dto.setEmailVerified(true);

		log.info("[{}]|SSO|TokenValidation|Mapped user details - userId: {}, email: {}, emailVerified: {}", traceId, dto.getUserId(), dto.getEmail(),
				dto.isEmailVerified());

		return dto;
	}

}
