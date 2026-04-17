package com.social.ripple.usermanagement.controller;

import java.util.Date;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dto.request.ToggleConnectionRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IConnectionService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("{version}/connections")
@Slf4j
public class ConnectionController {

	private final IConnectionService connectionService;

	public ConnectionController(IConnectionService connectionService) {
		this.connectionService = connectionService;
	}

	@PostMapping("/toggle")
	public ResponseEntity<BaseResponse> toggleConnection(@PathVariable("version") String version,
			@RequestHeader("x-trace-id") String traceId,
			@RequestHeader(name = "x-tenant-id", required = true) String tenantId,
			@RequestHeader(name = "x-correlation-id", required = false) String correlationId,
			@RequestHeader(name = "language-id", required = false) String languageId,
			@RequestHeader(name = "authorization", required = true) String authorization,
			@AuthenticationPrincipal UserDetailsImpl userDetails, @RequestBody ToggleConnectionRequest request) {

		log.info("[{}]|TOGGLE_CONNECTION|REQUEST_RECEIVED|Platform={} Connect={}", traceId, request.getPlatform(),
				request.isConnect());

		if (!validateUser(tenantId, userDetails, traceId)) {
			return buildErrorResponse(traceId, "401", "Unauthorized", "User's organization does not match tenant ID",
					HttpStatus.UNAUTHORIZED, new BaseResponse());
		}

		try {
			BaseResponse serviceResponse = connectionService.toggleConnection(userDetails.getUserId(), request,
					traceId);
			serviceResponse.setTimestamp(new Date());
			return new ResponseEntity<>(serviceResponse, resolveHttpStatus(serviceResponse.getCode()));
		} catch (Exception e) {
			log.error("[{}]|TOGGLE_CONNECTION|EXCEPTION|{}", traceId, e.getMessage(), e);
			return buildErrorResponse(traceId, "500", "Internal server error", e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR, new BaseResponse());
		}
	}

	private boolean validateUser(String tenantId, UserDetailsImpl userDetails, String traceId) {
		if (userDetails == null || userDetails.getOrganization() == null) {
			log.warn("[{}]|AUTH|USER_DETAILS_NULL", traceId);
			return false;
		}
		String userOrgId = String.valueOf(userDetails.getOrganization().getId());
		if (!Objects.equals(tenantId, userOrgId)) {
			log.warn("[{}]|AUTH|TENANT_MISMATCH|TokenOrg={}, HeaderOrg={}", traceId, userOrgId, tenantId);
			return false;
		}
		return true;
	}

	private ResponseEntity<BaseResponse> buildErrorResponse(String traceId, String code, String message,
			String devMessage, HttpStatus status, BaseResponse response) {
		log.warn("[{}]|ERROR_RESPONSE|code={} message={}", traceId, code, message);
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setTimestamp(new Date());
		return new ResponseEntity<>(response, status);
	}

	private HttpStatus resolveHttpStatus(String code) {
		if (code == null) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
		return switch (code) {
		case "400" -> HttpStatus.BAD_REQUEST;
		case "401" -> HttpStatus.UNAUTHORIZED;
		case "403" -> HttpStatus.FORBIDDEN;
		case "404" -> HttpStatus.NOT_FOUND;
		case "409" -> HttpStatus.CONFLICT;
		case "422" -> HttpStatus.UNPROCESSABLE_ENTITY;
		case "500" -> HttpStatus.INTERNAL_SERVER_ERROR;
		default -> HttpStatus.OK;
		};
	}
}
