package com.social.ripple.usermanagement.controller;

import com.social.ripple.usermanagement.dto.request.UserAdminToggleRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.social.ripple.usermanagement.dto.request.InviteUserRequest;
import com.social.ripple.usermanagement.dto.request.ReinviteUserRequest;
import com.social.ripple.usermanagement.dto.response.InviteUserResponse;
import com.social.ripple.usermanagement.service.IInviteUserService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping
@Slf4j
public class InviteUserController {

	private final IInviteUserService inviteUserService;

	public InviteUserController(IInviteUserService inviteUserService) {
		this.inviteUserService = inviteUserService;
	}

	@PostMapping("{version}/admin/invite")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<InviteUserResponse> inviteUser(@PathVariable("version") String version,@RequestHeader(name = "x-tenant-id", required = true)
	String tenantId, @RequestHeader(name = "x-trace-id", required = true)
	String traceId, @RequestHeader(name = "x-correlation-id", required = false)
	String correlationId, @RequestHeader(name = "language-id", required = false)
	String languageId, @RequestHeader(name = "authorization", required = true)
	String authorization, @RequestBody
	InviteUserRequest request) {

		log.info("[{}]|USMG|InviteUser|Start|tenantId:{}|email:{}", traceId, tenantId, request.getEmail());

		try {
			return inviteUserService.inviteUser(request, tenantId, traceId);
		}
		catch (Exception ex) {
			log.error("[{}]|USMG|InviteUser|UnexpectedError|message:{}", traceId, ex.getMessage(), ex);
			return ResponseEntity.status(500).build();
		}
	}

	@PostMapping("{version}/admin/reinvite")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<InviteUserResponse> reinviteUser(@PathVariable("version") String version,@RequestHeader(name = "x-tenant-id")
	String tenantId, @RequestHeader(name = "x-trace-id", required = true)
	String traceId, @RequestHeader(name = "x-correlation-id", required = false)
	String correlationId, @RequestHeader(name = "language-id", required = false)
	String languageId, @RequestHeader(name = "authorization", required = true)
	String authorization, @RequestBody
	ReinviteUserRequest request) {

		log.info("[{}]|USMG|ReinviteUser|Start|tenantId:{}|email:{}", traceId, tenantId, request.getEmail());

		try {
			return inviteUserService.reinviteUser(request, tenantId, traceId);
		}
		catch (Exception ex) {
			log.error("[{}]|USMG|ReinviteUser|UnexpectedError|message:{}", traceId, ex.getMessage(), ex);
			return ResponseEntity.status(500).build();
		}
	}

	@PutMapping("{version}/admin/users/update")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> updateUser(@PathVariable("version") String version, @RequestHeader(name = "x-tenant-id", required = true)
	String tenantId, @RequestHeader(name = "x-trace-id", required = true)
														 String traceId, @RequestHeader(name = "x-correlation-id", required = false)
														 String correlationId, @RequestHeader(name = "language-id", required = false)
														 String languageId, @RequestHeader(name = "authorization", required = true)
														 String authorization, @RequestBody
														 InviteUserRequest request) {

		log.info("[{}]|USMG|InviteUser|Start|tenantId:{}", traceId, tenantId);

		try {
			return inviteUserService.updateUser(request,tenantId,traceId);
		}
		catch (Exception ex) {
			log.error("[{}]|USMG|InviteUser|UnexpectedError|message:{}", traceId, ex.getMessage(), ex);
			return ResponseEntity.status(500).build();
		}
	}

	@PutMapping("{version}/admin/users/toggle-admin-role")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> toggleAdminRole(@PathVariable("version") String version, @RequestHeader(name = "x-tenant-id", required = true)
	String tenantId, @RequestHeader(name = "x-trace-id", required = true)
												   String traceId, @RequestHeader(name = "x-correlation-id", required = false)
												   String correlationId, @RequestHeader(name = "language-id", required = false)
												   String languageId, @RequestHeader(name = "authorization", required = true)
												   String authorization, @RequestBody
														UserAdminToggleRequest request) {

		log.info("[{}]|USMG|InviteUser|Start|tenantId:{}", traceId, tenantId);

		try {
			return inviteUserService.toggleAdminRole(request,tenantId,traceId);
		}
		catch (Exception ex) {
			log.error("[{}]|USMG|InviteUser|UnexpectedError|message:{}", traceId, ex.getMessage(), ex);
			return ResponseEntity.status(500).build();
		}
	}
}
