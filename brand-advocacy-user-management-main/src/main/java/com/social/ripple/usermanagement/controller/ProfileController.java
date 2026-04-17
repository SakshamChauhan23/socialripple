/**
 * Filename: ProfileController.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dto.ProfileRequestDTO;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ProfileDataResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IProfileService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping
public class ProfileController {

	private IProfileService profileService;

	@Autowired
	public ProfileController(IProfileService profileService) {
		this.profileService = profileService;
	}

	@GetMapping("{version}/user/profile")
	public ResponseEntity<ProfileDataResponse> viewProfile(
	    @PathVariable("version") String version,
	    @RequestHeader(value="x-trace-id", required = true) String traceId, 
	    @RequestHeader(value = "x-correlation-id", required = false) String xCorrelationId, 
	    @RequestHeader(value = "language-id", required = false) String languageId, 
	    @RequestHeader(value ="x-tenant-id", required = false) String tenantId, 
	    @AuthenticationPrincipal UserDetailsImpl userDetails,
	    @RequestParam(value = "userId", required = false) Long userId) {
	    
	    log.info("[{}]|PROFILE|GET|Start profile fetch", traceId);

	    if (!StringUtils.hasText(traceId) || !StringUtils.hasText(tenantId)) {
	        log.warn("[{}]|PROFILE|HEADER_VALIDATION|Missing trace-id or tenant-id", traceId);
	        ProfileDataResponse errorResponse = new ProfileDataResponse();
	        errorResponse.setTimestamp(new Date());
	        errorResponse.setStatus(false);
	        errorResponse.setCode(ResponseCode.USMG_400);
	        errorResponse.setMessage("Missing mandatory headers");
	        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
	    }

	    ProfileDataResponse response = profileService.viewProfile(traceId, userId, userDetails);
	    return ResponseEntity.status(resolveHttpStatus(response.getCode())).body(response);
	}

	@PutMapping("{version}/user/profile")
	public ResponseEntity<BaseResponse> updateProfile(
			@PathVariable("version") String version,
			@RequestHeader(value="x-trace-id" ,required = true)String traceId, 
			@RequestHeader(value = "x-correlation-id", required = false)String xCorrelationId, 
			@RequestHeader(value = "language-id", required = false)String languageId, 
			@RequestHeader(value ="x-tenant-id", required=false)String tenantId, 
			@AuthenticationPrincipal UserDetailsImpl userDetails , 
			@RequestBody ProfileRequestDTO request) {
		log.info("[{}]|PROFILE|UPDATE|Start profile update", traceId);
		log.debug("[{}]|PROFILE|UPDATE|Payload: {}", traceId, request);

		if (!StringUtils.hasText(traceId) || !StringUtils.hasText(tenantId)) {
			log.warn("[{}]|PROFILE|HEADER_VALIDATION|Missing trace-id or tenant-id", traceId);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}

		BaseResponse response = profileService.updateProfile(traceId, request, userDetails);
		return ResponseEntity.status(resolveHttpStatus(response.getCode())).body(response);
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