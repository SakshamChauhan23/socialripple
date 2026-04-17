
/**
 * Filename: AuthController.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
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
package com.social.ripple.usermanagement.controller;

import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dto.request.LoginRequest;
import com.social.ripple.usermanagement.dto.request.OrganizationOnboardingRequest;
import com.social.ripple.usermanagement.dto.request.RefreshTokenRequest;
import com.social.ripple.usermanagement.dto.request.SignupRequest;
import com.social.ripple.usermanagement.dto.response.AuthResponse;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.LoginResponse;
import com.social.ripple.usermanagement.dto.response.SignupResponse;
import com.social.ripple.usermanagement.dto.response.SsoProviderResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IAuthService;
import com.social.ripple.usermanagement.util.AppCommonValidator;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.extern.slf4j.Slf4j;
@RestController
@RequestMapping
@Slf4j
public class AuthController {

	private final IAuthService authService;
	private final AppCommonValidator commonValidator;

	public AuthController(IAuthService authService, AppCommonValidator commonValidator) {
		this.authService = authService;
		this.commonValidator = commonValidator;
	}

	@PostMapping("{version}/auth/login")
	public ResponseEntity<BaseResponse> login(@PathVariable("version")
	String version, @RequestHeader(value = "x-trace-id", required = false)
	String xTraceId, @RequestHeader(value = "Language_id", required = false)
	String languageId, @RequestHeader(value = "x-correlation-id", required = false)
	String xCorrelationId, @RequestHeader(value = "Authorization", required = false)
	String authorization, @RequestHeader(value = "tenant-id", required = false)
	String tenantId, @RequestBody
	LoginRequest loginRequest) {
		xTraceId = commonValidator.getOrGenerateRequestId(xTraceId);

		log.info("[{}]|auth|login|request_received|email:{}", xTraceId, loginRequest.getUserName());

		try {
			LoginResponse response = authService.login(xTraceId, xCorrelationId, loginRequest);

			if (!response.isStatus()) {
				log.warn("[{}]|auth|login|failure|email:{}|code:{}|message:{}", xTraceId, loginRequest.getUserName(), response.getCode(),
						response.getMessage());

				HttpStatus status = "USMG_401".equals(response.getCode()) ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;

				return buildErrorResponse(xTraceId, response.getCode(), response.getMessage(), response.getDevMessage(), status);
			}

			log.info("[{}]|auth|login|success|email:{}", xTraceId, loginRequest.getUserName());
			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			log.error("[{}]|auth|login|exception|email:{}|error:{}", xTraceId, loginRequest.getUserName(), e.getMessage(), e);
			return buildErrorResponse(xTraceId, ResponseCode.USMG_500, "Internal server error", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("{version}/auth/sso/providers")
	public ResponseEntity<BaseResponse> ssoProviders(@PathVariable("version")
	String version, @RequestHeader(value = "x-trace-id")
	String xTraceId, @RequestHeader(value = "Language-id", required = false)
	String languageId, @RequestHeader(value = "x-correlation-id", required = false)
	String xCorrelationId, @RequestHeader(value = "Authorization", required = false)
	String authorization, @RequestParam(value = "captchaToken", required = true)
	String captchaToken) {

		log.info("[{}]|auth|sso-providers|request_received", xTraceId);

		try {
			SsoProviderResponse response = authService.ssoProviders(xTraceId, captchaToken);

			if (!response.isStatus()) {
				log.warn("[{}]|auth|sso-providers|failure|code:{}|message:{}", xTraceId, response.getCode(), response.getMessage());
				HttpStatus status = ResponseCode.USMG_403.equals(response.getCode()) ? HttpStatus.FORBIDDEN : HttpStatus.INTERNAL_SERVER_ERROR;
				return buildErrorResponse(xTraceId, response.getCode(), response.getMessage(), response.getDevMessage(), status);
			}

			log.info("[{}]|auth|sso-providers|success|provider_count:{}", xTraceId, response.getProviderData().size());
			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			log.error("[{}]|auth|sso-providers|exception|error:{}", xTraceId, e.getMessage(), e);
			return buildErrorResponse(xTraceId, ResponseCode.USMG_500, "Internal server error", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private ResponseEntity<BaseResponse> buildErrorResponse(String traceId, String code, String message, String devMessage, HttpStatus status) {
		BaseResponse baseResponse = new BaseResponse();
		baseResponse.setStatus(false);
		baseResponse.setCode(code);
		baseResponse.setMessage(message);
		baseResponse.setDevMessage(devMessage);
		baseResponse.setTimestamp(new Date());
		baseResponse.setErrors(List.of(new ErrorObj(traceId, code, message, devMessage)));

		return new ResponseEntity<>(baseResponse, status);
	}

	@GetMapping("{version}/auth/invite-info")
	public ResponseEntity<?> getInviteInfo(@PathVariable("version") String version,
			@RequestParam("token") String token,
			@RequestHeader(name = "x-trace-id", required = false) String traceId) {
		if (traceId == null || traceId.isBlank()) traceId = java.util.UUID.randomUUID().toString();
		try {
			return authService.getInviteInfo(token, traceId);
		} catch (Exception ex) {
			log.error("[{}]|USMG|InviteInfo|Error|{}", traceId, ex.getMessage(), ex);
			return ResponseEntity.status(500).body(java.util.Map.of("status", false, "message", "Failed to fetch invite info"));
		}
	}

	@PostMapping("{version}/auth/signup")
	public ResponseEntity<SignupResponse> signupViaInvitation(@PathVariable("version")
	String version, @RequestHeader(name = "x-tenant-id", required = false)
	String tenantId, @RequestHeader(name = "x-trace-id")
	String traceId, @RequestHeader(name = "x-correlation-id", required = false)
	String correlationId, @RequestHeader(name = "language-id", required = false)
	String languageId, @RequestHeader(name = "channel", required = false)
	String channel, @RequestBody
	SignupRequest request) {

		log.info("[{}]|USMG|Signup|Start|tenantId:{}|email:{}", traceId, tenantId, request.getEmail());

		try {
			return authService.signupViaInvitation(request, tenantId, traceId);
		}
		catch (Exception ex) {
			log.error("[{}]|USMG|Signup|UnexpectedError|message:{}", traceId, ex.getMessage(), ex);
			return ResponseEntity.status(500).build();
		}
	}

	@PostMapping("{version}/auth/register")
	public ResponseEntity<SignupResponse> register(@PathVariable("version")
	String version, @RequestHeader(name = "x-trace-id")
	String traceId, @RequestHeader(name = "x-correlation-id", required = false)
	String correlationId, @RequestHeader(name = "language-id", required = false)
	String languageId, @RequestHeader(name = "channel", required = false)
	String channel, @RequestBody
	SignupRequest request) {

		log.info("[{}]|USMG|Register|Start|email:{}", traceId, request.getEmail());

		try {
			return authService.register(request, traceId);
		}
		catch (Exception ex) {
			log.error("[{}]|USMG|Register|UnexpectedError|message:{}", traceId, ex.getMessage(), ex);
			return ResponseEntity.status(500).build();
		}
	}

	@PostMapping("{version}/organizations/onboarding")
	public ResponseEntity<BaseResponse> completeOrganizationOnboarding(@PathVariable("version")
	String version, @RequestHeader(name = "x-trace-id")
	String traceId, @AuthenticationPrincipal
	UserDetailsImpl userDetails, @RequestBody
	OrganizationOnboardingRequest request) {
		log.info("[{}]|USMG|OrganizationOnboarding|Start|user:{}", traceId, userDetails != null ? userDetails.getUsername() : "null");
		try {
			AuthResponse response = authService.completeOrganizationOnboarding(traceId, userDetails, request);
			if (!response.isStatus()) {
				HttpStatus status = switch (response.getCode()) {
					case ResponseCode.USMG_400 -> HttpStatus.BAD_REQUEST;
					case ResponseCode.USMG_401 -> HttpStatus.UNAUTHORIZED;
					case ResponseCode.USMG_409 -> HttpStatus.CONFLICT;
					case ResponseCode.USMG_404 -> HttpStatus.NOT_FOUND;
					default -> HttpStatus.INTERNAL_SERVER_ERROR;
				};
				return ResponseEntity.status(status).body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception ex) {
			log.error("[{}]|USMG|OrganizationOnboarding|UnexpectedError|message:{}", traceId, ex.getMessage(), ex);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Internal server error", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("{version}/auth/token/refresh")
	public ResponseEntity<BaseResponse> refreshToken(@PathVariable
	String version, @RequestHeader(value = "x-trace-id", required = true)
	String xTraceId, @RequestHeader(value = "language-id", required = false)
	String languageId, @RequestHeader(value = "x-correlation-id", required = false)
	String xCorrelationId, @RequestHeader(value = "x-tenant-id", required = false)
	String tenantId, @RequestBody
	RefreshTokenRequest tokenRequest) {

		log.info("[{}]|auth|refresh-token|request_received|userId:{}", xTraceId);

		try {
			AuthResponse response = authService.refreshToken(xTraceId, tenantId, tokenRequest);

			if (!response.isStatus()) {
				log.warn("[{}]|auth|refresh-token|failure|code:{}|message:{}", xTraceId, response.getCode(), response.getMessage());
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}

			log.info("[{}]|auth|refresh-token|success", xTraceId);
			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			log.error("[{}]|auth|refresh-token|exception|error:{}", xTraceId, e.getMessage(), e);
			BaseResponse errorResponse = BaseResponse.builder()
					.status(false)
					.code("USMG_500")
					.message("Internal server error")
					.timestamp(new Date())
					.build();

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}
}
