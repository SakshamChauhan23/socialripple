package com.social.ripple.usermanagement.controller;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dto.response.GetAllRolesResponse;
import com.social.ripple.usermanagement.dto.response.GetAllUsersResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IReferenceDataService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/{version}/reference")
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataController {

	private final IReferenceDataService referenceDataService;

	@GetMapping("/roles/{organizationId}")
	public ResponseEntity<GetAllRolesResponse> getRolesByOrganization(@PathVariable("version") String version,
			@PathVariable("organizationId") Long organizationId, @RequestHeader("x-trace-id") String traceId,
			@RequestHeader(name = "x-tenant-id", required = true) String tenantId,
			@RequestHeader(name = "x-correlation-id", required = false) String correlationId,
			@RequestHeader(name = "language-id", required = false) String languageId,
			@RequestHeader(name = "authorization", required = true) String authorization,
			@AuthenticationPrincipal UserDetailsImpl userDetails) {

		log.info("[{}]|GET_ROLES_BY_ORG|REQUEST_RECEIVED|organizationId:{}|tenantId:{}", traceId, organizationId,
				tenantId);

		try {
			GetAllRolesResponse serviceResponse = referenceDataService.getRolesByOrganization(traceId, tenantId,
					organizationId);
			return new ResponseEntity<>(serviceResponse, HttpStatus.OK);
		} catch (Exception e) {
			log.error("[{}]|GET_ROLES_BY_ORG|EXCEPTION|{}", traceId, e.getMessage(), e);
			return buildRolesErrorResponse(traceId, ResponseCode.USMG_500, "Internal server error",
					HttpStatus.INTERNAL_SERVER_ERROR, new GetAllRolesResponse());
		}
	}

	@GetMapping("/users/{organizationId}")
	public ResponseEntity<GetAllUsersResponse> listAllUsersByOrganization(@PathVariable("version") String version,
			@PathVariable("organizationId") Long organizationId, @RequestHeader("x-trace-id") String traceId,
			@RequestHeader("x-tenant-id") String tenantId,
			@RequestHeader(name = "x-correlation-id", required = false) String correlationId,
			@RequestHeader(name = "language-id", required = false) String languageId,
			@RequestHeader(name = "authorization", required = true) String authorization,
			@AuthenticationPrincipal UserDetailsImpl userDetails) {

		log.info("[{}]|GET_USERS_BY_ORG|REQUEST_RECEIVED|organizationId:{}|tenantId:{}", traceId, organizationId,
				tenantId);

		try {
			GetAllUsersResponse serviceResponse = referenceDataService.listAllUsersByOrganization(traceId, tenantId,
					organizationId);
			return new ResponseEntity<>(serviceResponse, HttpStatus.OK);
		} catch (Exception e) {
			log.error("[{}]|GET_USERS_BY_ORG|EXCEPTION|{}", traceId, e.getMessage(), e);
			return buildUsersErrorResponse(traceId, ResponseCode.USMG_500, "Internal server error",
					HttpStatus.INTERNAL_SERVER_ERROR, new GetAllUsersResponse());
		}
	}

	private ResponseEntity<GetAllRolesResponse> buildRolesErrorResponse(String traceId, String code, String message,
			HttpStatus status, GetAllRolesResponse response) {

		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setTimestamp(new Date());

		log.info("[{}]|ERROR_RESPONSE|code:{}|message:{}", traceId, code, message);
		return new ResponseEntity<>(response, status);
	}

	private ResponseEntity<GetAllUsersResponse> buildUsersErrorResponse(String traceId, String code, String message,
			HttpStatus status, GetAllUsersResponse response) {

		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setTimestamp(new Date());

		log.info("[{}]|ERROR_RESPONSE|code:{}|message:{}", traceId, code, message);
		return new ResponseEntity<>(response, status);
	}
}
