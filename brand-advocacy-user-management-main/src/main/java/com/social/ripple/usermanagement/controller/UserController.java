package com.social.ripple.usermanagement.controller;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.social.ripple.usermanagement.dto.request.UpdateUserStatusRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.GetAllUsersResponse;
import com.social.ripple.usermanagement.dto.response.UpdateUserStatusResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IUserService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

@RestController
@RequestMapping("{version}/users")
public class UserController {

	private final IUserService userService;

	public UserController(IUserService userService) {
		this.userService = userService;
	}

	@GetMapping("/organization/{organizationId}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> getUsersByOrganization(@PathVariable("version") String version,
			@PathVariable("organizationId") Long organizationId,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestHeader(value = "x-trace-id") String traceId, @RequestHeader(value = "x-tenant-id") String tenantId,
			@RequestHeader(value = "x-correlation-id", required = false) String correlationId,
			@RequestHeader(value = "language-id", required = false) String languageId) {

		try {
			Pageable pageable = PageRequest.of(page, size);

			ResponseEntity<GetAllUsersResponse> serviceResponse = userService.getUsersByOrganization(traceId,
					organizationId, tenantId, userDetails, pageable);

			GetAllUsersResponse body = serviceResponse.getBody();
			if (body != null) {
				body.setTimestamp(new Date());
			}

			return ResponseEntity.status(resolveHttpStatus(body != null ? body.getCode() : ResponseCode.USMG_500))
					.body(body);

		} catch (Exception e) {
			return buildErrorResponse("/users/organization/" + organizationId, ResponseCode.USMG_500,
					"Internal server error", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PutMapping("/{userId}/toggle/status")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> toggleUserStatus(@PathVariable("version") String version,
			@PathVariable("userId") Long userId, @RequestBody UpdateUserStatusRequest request,
			@AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestHeader(value = "x-trace-id") String traceId, @RequestHeader(value = "x-tenant-id") String tenantId,
			@RequestHeader(value = "x-correlation-id", required = false) String correlationId,
			@RequestHeader(value = "language-id", required = false) String languageId) {

		try {
			ResponseEntity<UpdateUserStatusResponse> serviceResponse = userService
					.ToggleUserStatus(traceId, userId,
					request, tenantId, userDetails);

			UpdateUserStatusResponse body = serviceResponse.getBody();
			if (body != null) {
				body.setTimestamp(new Date());
			}

			return ResponseEntity.status(resolveHttpStatus(body != null ? body.getCode() : ResponseCode.USMG_500))
					.body(body);

		} catch (Exception e) {
			return buildErrorResponse("/users/" + userId + "/status", ResponseCode.USMG_500, "Internal server error",
					e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping("/{userId}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> removeUser(@PathVariable("version") String version,
														 @PathVariable("userId") Long userId,
														 @AuthenticationPrincipal UserDetailsImpl userDetails,
														 @RequestHeader(value = "x-trace-id") String traceId, @RequestHeader(value = "x-tenant-id") String tenantId,
														 @RequestHeader(value = "x-correlation-id", required = false) String correlationId,
														 @RequestHeader(value = "language-id", required = false) String languageId) {

		try {
			ResponseEntity<UpdateUserStatusResponse> serviceResponse = userService
					.removeUser(traceId, userId,
							tenantId, userDetails);

			UpdateUserStatusResponse body = serviceResponse.getBody();
			if (body != null) {
				body.setTimestamp(new Date());
			}

			return ResponseEntity.status(resolveHttpStatus(body != null ? body.getCode() : ResponseCode.USMG_500))
					.body(body);

		} catch (Exception e) {
			return buildErrorResponse("/users/" + userId + "/status", ResponseCode.USMG_500, "Internal server error",
					e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private ResponseEntity<BaseResponse> buildErrorResponse(String path, String code, String error, String message,
			HttpStatus status) {

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
