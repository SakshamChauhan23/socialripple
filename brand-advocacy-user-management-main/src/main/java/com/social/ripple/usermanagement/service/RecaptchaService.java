/**
 * Filename: RecaptchaService.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
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
package com.social.ripple.usermanagement.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.social.ripple.usermanagement.dto.response.RecaptchaResponse;
import com.social.ripple.usermanagement.util.AppCache;
import com.social.ripple.usermanagement.util.ConfigKeys;
import com.social.ripple.usermanagement.util.constants.ApplicationConstants;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RecaptchaService implements ICaptchaService {

	private static final String MODULE = "RECAPTCHA";

	private final RestTemplate restTemplate;
	private final String recaptchaSecretKeyOverride;
	private final String recaptchaVerifyUrlOverride;

	public RecaptchaService(
			@Value("${app.security.recaptcha.secret-key:}") String recaptchaSecretKeyOverride,
			@Value("${app.security.recaptcha.verify-url:}") String recaptchaVerifyUrlOverride) {
		this.restTemplate = new RestTemplate();
		this.recaptchaSecretKeyOverride = recaptchaSecretKeyOverride;
		this.recaptchaVerifyUrlOverride = recaptchaVerifyUrlOverride;
	}

	@Override
	public RecaptchaResponse verifyToken(String traceId, String token) {
		log.info("[{}]|{}|VERIFY_TOKEN|INITIATED|Verifying reCAPTCHA token", traceId, MODULE);

		if (token == null || token.isBlank()) {
			log.warn("[{}]|{}|VERIFY_TOKEN|FAILED|Token is null or empty", traceId, MODULE);
			return buildErrorResponse("invalid-token");
		}
		try {
			HttpEntity<MultiValueMap<String, String>> requestEntity = buildRequestEntity(traceId, token);
			ResponseEntity<RecaptchaResponse> responseEntity = restTemplate.postForEntity(
					getConfig(traceId, ConfigKeys.RECAPTCHA_VERIFY_URL, recaptchaVerifyUrlOverride, ApplicationConstants.RECAPTCHA_VERIFY_URL), requestEntity,
					RecaptchaResponse.class);

			RecaptchaResponse recaptchaResponse = Optional.ofNullable(responseEntity.getBody()).orElseGet(() -> {
				log.warn("[{}]|{}|VERIFY_TOKEN|FAILED|Empty response from reCAPTCHA server", traceId, MODULE);
				return buildErrorResponse("empty-response");
			});

			if (recaptchaResponse.isSuccess()) {
				log.info("[{}]|{}|VERIFY_TOKEN|SUCCESS|success={}, score={}, action={}, hostname={}", traceId, MODULE,
						recaptchaResponse.isSuccess(), recaptchaResponse.getScore(), recaptchaResponse.getAction(), recaptchaResponse.getHostname());
			}
			else {
				log.warn("[{}]|{}|VERIFY_TOKEN|FAILED|success={}, score={}, action={}, hostname={}, errorCodes={}", traceId, MODULE,
						recaptchaResponse.isSuccess(), recaptchaResponse.getScore(), recaptchaResponse.getAction(), recaptchaResponse.getHostname(),
						recaptchaResponse.getErrorCodes());
			}

			return recaptchaResponse;
		}
		catch (RestClientException ex) {
			log.error("[{}]|{}|VERIFY_TOKEN|EXCEPTION|RestClientException occurred: {}", traceId, MODULE, ex.getMessage(), ex);
			return buildErrorResponse("recaptcha-exception");
		}
		catch (Exception ex) {
			log.error("[{}]|{}|VERIFY_TOKEN|EXCEPTION|Unexpected error: {}", traceId, MODULE, ex.getMessage(), ex);
			return buildErrorResponse("unexpected-error");
		}
	}

	private HttpEntity<MultiValueMap<String, String>> buildRequestEntity(String traceId, String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
		requestBody.add("secret", getConfig(traceId, ConfigKeys.RECAPTCHA_SECRET_KEY, recaptchaSecretKeyOverride, null));
		requestBody.add("response", token);

		return new HttpEntity<>(requestBody, headers);
	}

	private String getConfig(String traceId, String key, String overrideValue, String defaultValue) {
		if (overrideValue != null && !overrideValue.isBlank()) {
			return overrideValue;
		}

		return Optional.ofNullable(AppCache.configParameters.get(key)).map(param -> {
			String val = param.getConfigValue();
			if (val == null || val.isEmpty()) {
				log.error("[{}]|{}|CONFIG_LOOKUP|NULL_OR_EMPTY|Key '{}' missing or empty. Using default '{}'", traceId, MODULE, key, defaultValue);
			}
			return val;
		}).filter(val -> val != null && !val.isEmpty()).orElse(defaultValue);
	}

	private RecaptchaResponse buildErrorResponse(String errorCode) {
		return RecaptchaResponse.builder().success(false).score(0.0f).errorCodes(List.of(errorCode)).action(null).build();
	}
}
