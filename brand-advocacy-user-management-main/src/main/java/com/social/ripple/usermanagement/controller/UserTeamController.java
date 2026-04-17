package com.social.ripple.usermanagement.controller;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.social.ripple.usermanagement.dto.request.AddTeamMemberRequest;
import com.social.ripple.usermanagement.dto.request.RemoveTeamMemberRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IUserTeamService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("{version}")
@Slf4j
public class UserTeamController {

	private final IUserTeamService userTeamService;

	public UserTeamController(IUserTeamService userTeamService) {
		this.userTeamService = userTeamService;
	}

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@PostMapping("/admin/teams/{teamId}/members/add")
	public ResponseEntity<BaseResponse> addTeamMembers(@PathVariable("version")
	String version, @PathVariable
	Long teamId, @RequestHeader("x-trace-id")
	String traceId, @RequestHeader("x-tenant-id")
	String tenantId, @RequestHeader(name = "authorization")
	String authorization, @AuthenticationPrincipal
	UserDetailsImpl userDetails, @RequestBody
	AddTeamMemberRequest request) {

		log.info("[{}]|ADD_TEAM_MEMBERS|teamId={}", traceId, teamId);

		try {
			if (!validateUser(tenantId, userDetails, traceId)) {
				return buildErrorResponse(traceId, ResponseCode.USMG_401, "Unauthorized", "User's organization does not match tenant ID",
						HttpStatus.UNAUTHORIZED);
			}
			return userTeamService.addMembersToTeam(teamId, tenantId, request, traceId);

		}
		catch (Exception e) {
			log.error("[{}]|ADD_TEAM_MEMBERS|EXCEPTION|{}", traceId, e.getMessage(), e);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Internal server error", "Exception occurred while adding members to team",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@DeleteMapping("/admin/teams/members/remove")
	public ResponseEntity<BaseResponse> removeTeamMember(@PathVariable("version")
	String version, @RequestHeader("x-trace-id")
	String traceId, @RequestHeader("x-tenant-id")
	String tenantId, @RequestHeader(name = "authorization")
	String authorization, @AuthenticationPrincipal
	UserDetailsImpl userDetails, @RequestBody
	RemoveTeamMemberRequest request) {

		try {
			Long teamId = request.getTeamId();
			Long userId = request.getUserId();

			log.info("[{}]|REMOVE_TEAM_MEMBER|REQUEST_RECEIVED|teamId={} userId={}", traceId, teamId, userId);

			if (teamId == null || userId == null) {
				return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request", "Missing teamId or userId in request body",
						HttpStatus.BAD_REQUEST);
			}

			if (!validateUser(tenantId, userDetails, traceId)) {
				return buildErrorResponse(traceId, ResponseCode.USMG_401, "Unauthorized", "User's organization does not match tenant ID",
						HttpStatus.UNAUTHORIZED);
			}

			return userTeamService.removeMemberFromTeam(request, tenantId, traceId);

		}
		catch (Exception ex) {
			log.error("[{}]|REMOVE_TEAM_MEMBER|EXCEPTION|{}", traceId, ex.getMessage(), ex);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Internal Server Error", "Something went wrong while removing team member",
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private boolean validateUser(String tenantId, UserDetailsImpl userDetails, String traceId) {
		if (userDetails == null || userDetails.getOrganization() == null) {
			log.warn("[{}]|AUTH|USER_DETAILS_NULL", traceId);
			return false;
		}
		String userOrgId = String.valueOf(userDetails.getOrganization().getId());
		if (!Objects.equals(tenantId, userOrgId)) {
			log.warn("[{}]|AUTH|TENANT_MISMATCH|TokenOrg={} HeaderOrg={}", traceId, userOrgId, tenantId);
			return false;
		}
		return true;
	}

	private ResponseEntity<BaseResponse> buildErrorResponse(String traceId, String code, String message, String devMessage, HttpStatus status) {

		log.warn("[{}]|ERROR_RESPONSE|code={} message={} devMessage={}", traceId, code, message, devMessage);

		BaseResponse response = new BaseResponse();
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setTimestamp(new Date());
		response.setErrors(List.of(new ErrorObj(null, code, message, devMessage)));

		return new ResponseEntity<>(response, status);
	}
}
