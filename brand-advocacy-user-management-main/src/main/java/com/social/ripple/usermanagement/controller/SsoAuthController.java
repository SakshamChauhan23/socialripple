/**
 * Filename: SsoAuthController.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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
package com.social.ripple.usermanagement.controller;

import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dto.request.SsoAuthRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.service.ISsoAuthService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

@RestController
@RequestMapping
public class SsoAuthController {

	private final ISsoAuthService ssoAuthService;

	public SsoAuthController(ISsoAuthService ssoAuthService) {
		this.ssoAuthService = ssoAuthService;
	}

	@PostMapping("{version}/auth/sso/signup")
	public ResponseEntity<BaseResponse> signUp(@PathVariable("version")
	String version, @RequestHeader(value = "x-trace-id", required = true)
	String traceId, @RequestBody()
	SsoAuthRequest authRequest) {
		try {
			BaseResponse response = ssoAuthService.ssoSignUp(traceId, authRequest);
			response.setTimestamp(new Date());
			return ResponseEntity.status(resolveHttpStatus(response.getCode())).body(response);
		}
		catch (Exception e) {
			return buildErrorResponse("/auth/signup/sso", ResponseCode.USMG_500, "Internal server error", e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("{version}/auth/sso/register")
	public ResponseEntity<BaseResponse> register(@PathVariable("version")
	String version, @RequestHeader(value = "x-trace-id", required = true)
	String traceId, @RequestBody()
	SsoAuthRequest authRequest) {
		try {
			BaseResponse response = ssoAuthService.ssoRegister(traceId, authRequest);
			response.setTimestamp(new Date());
			return ResponseEntity.status(resolveHttpStatus(response.getCode())).body(response);
		}
		catch (Exception e) {
			return buildErrorResponse("/auth/register/sso", ResponseCode.USMG_500, "Internal server error", e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("{version}/auth/sso/signin")
	public ResponseEntity<BaseResponse> signIn(@PathVariable("version")
	String version, @RequestHeader(value = "x-trace-id", required = true)
	String traceId, @RequestBody()
	SsoAuthRequest authRequest) {
		try {
			BaseResponse response = ssoAuthService.ssoSignIn(traceId, authRequest);
			response.setTimestamp(new Date());
			return ResponseEntity.status(resolveHttpStatus(response.getCode())).body(response);
		}
		catch (Exception e) {
			return buildErrorResponse("/sso/signin", ResponseCode.USMG_500, "Internal server error", e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private ResponseEntity<BaseResponse> buildErrorResponse(String path, String code, String error, String message, HttpStatus status) {
		ErrorObj errorObj = new ErrorObj(path, code, error, message);
		BaseResponse baseResponse = new BaseResponse();
		baseResponse.setStatus(false);
		baseResponse.setCode(code);
		baseResponse.setMessage(error);
		baseResponse.setDevMessage(message);
		baseResponse.setTimestamp(new Date());
		baseResponse.setErrors(List.of(errorObj));
		return new ResponseEntity<>(baseResponse, status);
	}

	private HttpStatus resolveHttpStatus(String code) {
		return switch (code) {
			case ResponseCode.USMG_400 -> HttpStatus.BAD_REQUEST;
			case ResponseCode.USMG_401 -> HttpStatus.UNAUTHORIZED;
			case ResponseCode.USMG_403 -> HttpStatus.FORBIDDEN;
			case ResponseCode.USMG_404 -> HttpStatus.NOT_FOUND;
			case ResponseCode.USMG_409 -> HttpStatus.CONFLICT;
			case ResponseCode.USMG_422 -> HttpStatus.UNPROCESSABLE_ENTITY;
			case ResponseCode.USMG_423 -> HttpStatus.LOCKED;
			case ResponseCode.USMG_429 -> HttpStatus.TOO_MANY_REQUESTS;
			case ResponseCode.USMG_500 -> HttpStatus.INTERNAL_SERVER_ERROR;
			case ResponseCode.USMG_503 -> HttpStatus.SERVICE_UNAVAILABLE;
			case ResponseCode.USMG_504 -> HttpStatus.GATEWAY_TIMEOUT;
			default -> HttpStatus.OK;
		};
	}

}
