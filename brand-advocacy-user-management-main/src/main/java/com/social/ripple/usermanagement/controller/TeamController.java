package com.social.ripple.usermanagement.controller;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dto.request.CreateTeamRequest;
import com.social.ripple.usermanagement.dto.request.ToggleUser;
import com.social.ripple.usermanagement.dto.request.UpdateTeamRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.CreateTeamResponse;
import com.social.ripple.usermanagement.dto.response.DeleteTeamResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.GetAllTeamsResponse;
import com.social.ripple.usermanagement.dto.response.GetTeamDetailsResponse;
import com.social.ripple.usermanagement.dto.response.UpdateTeamResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.ITeamService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("{version}")
@Slf4j
public class TeamController {

	private final ITeamService teamService;

	public TeamController(ITeamService teamService) {
		this.teamService = teamService;
	}

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@PostMapping("/admin/teams/create")

	public ResponseEntity<CreateTeamResponse> createTeam(@PathVariable("version")
	String version, @RequestHeader("x-trace-id")
	String traceId, @RequestHeader("x-tenant-id")
	String tenantId, @RequestHeader(name = "x-correlation-id", required = false)
	String correlationId, @RequestHeader(name = "language-id", required = false)
	String languageId, @RequestHeader(name = "authorization", required = true)
	String authorization, @AuthenticationPrincipal
	UserDetailsImpl userDetails, @RequestBody
	CreateTeamRequest request) {

		log.info("[{}]|CREATE_TEAM|REQUEST_RECEIVED", traceId);

		if (!validateUser(tenantId, userDetails, traceId)) {
			return buildErrorResponse(traceId, ResponseCode.USMG_401, "Unauthorized", "User's organization does not match tenant ID",
					HttpStatus.UNAUTHORIZED, new CreateTeamResponse());
		}

		try {
			return teamService.createTeam(request, tenantId, String.valueOf(userDetails.getUserId()), traceId);
		}
		catch (Exception e) {
			log.error("[{}]|CREATE_TEAM|EXCEPTION|{}", traceId, e.getMessage(), e);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Internal server error", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR,
					new CreateTeamResponse());
		}
	}

	@GetMapping("/teams/list")

	public ResponseEntity<GetAllTeamsResponse> getAllTeams(@PathVariable("version")
	String version, @RequestHeader("x-trace-id")
	String traceId, @RequestHeader("x-tenant-id")
	String tenantId, @RequestHeader(name = "x-correlation-id", required = false)
	String correlationId, @RequestHeader(name = "language-id", required = false)
	String languageId, @RequestHeader(name = "authorization", required = true)
	String authorization, @AuthenticationPrincipal
	UserDetailsImpl userDetails) {

		log.info("[{}]|GET_ALL_TEAMS|REQUEST_RECEIVED", traceId);

		if (!validateUser(tenantId, userDetails, traceId)) {
			return buildErrorResponse(traceId, ResponseCode.USMG_401, "Unauthorized", "User's organization does not match tenant ID",
					HttpStatus.UNAUTHORIZED, new GetAllTeamsResponse());
		}

		try {
			return teamService.getAllTeams(tenantId, traceId);
		}
		catch (Exception e) {
			log.error("[{}]|GET_ALL_TEAMS|EXCEPTION|{}", traceId, e.getMessage(), e);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Internal server error", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR,
					new GetAllTeamsResponse());
		}
	}

	@GetMapping("/teams/details/{teamId}")

	public ResponseEntity<GetTeamDetailsResponse> getTeamDetails(@PathVariable("version")
	String version, @PathVariable("teamId")
	Long teamId, @RequestHeader("x-trace-id")
	String traceId, @RequestHeader("x-tenant-id")
	String tenantId, @RequestHeader(name = "x-correlation-id", required = false)
	String correlationId, @RequestHeader(name = "language-id", required = false)
	String languageId, @RequestHeader(name = "authorization", required = true)
	String authorization,

			@AuthenticationPrincipal
			UserDetailsImpl userDetails) {

		log.info("[{}]|GET_TEAM_DETAILS|teamId={}", traceId, teamId);

		if (!validateUser(tenantId, userDetails, traceId)) {
			return buildErrorResponse(traceId, ResponseCode.USMG_401, "Unauthorized", "User's organization does not match tenant ID",
					HttpStatus.UNAUTHORIZED, new GetTeamDetailsResponse());
		}

		try {
			return teamService.getTeamDetailsById(tenantId, teamId, traceId);
		}
		catch (Exception e) {
			log.error("[{}]|GET_TEAM_DETAILS|EXCEPTION|{}", traceId, e.getMessage(), e);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Internal server error", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR,
					new GetTeamDetailsResponse());
		}
	}

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@PutMapping("/admin/teams/update/{teamId}")

	public ResponseEntity<UpdateTeamResponse> updateTeam(@PathVariable("version")
	String version, @PathVariable("teamId")
	Long teamId, @RequestHeader("x-trace-id")
	String traceId, @RequestHeader(name = "x-correlation-id", required = false)
	String correlationId, @RequestHeader(name = "language-id", required = false)
	String languageId, @RequestHeader(name = "authorization", required = true)
	String authorization, @RequestHeader("x-tenant-id")
	String tenantId, @AuthenticationPrincipal
	UserDetailsImpl userDetails, @RequestBody
	UpdateTeamRequest request) {

		log.info("[{}]|UPDATE_TEAM|teamId={}", traceId, teamId);

		if (!validateUser(tenantId, userDetails, traceId)) {
			return buildErrorResponse(traceId, ResponseCode.USMG_401, "Unauthorized", "User's organization does not match tenant ID",
					HttpStatus.UNAUTHORIZED, new UpdateTeamResponse());
		}

		try {
			return teamService.updateTeam(request, tenantId, teamId, traceId);
		}
		catch (Exception e) {
			log.error("[{}]|UPDATE_TEAM|EXCEPTION|{}", traceId, e.getMessage(), e);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Internal server error", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR,
					new UpdateTeamResponse());
		}
	}

	@PreAuthorize("hasRole('ROLE_ADMIN')")
	@DeleteMapping("/admin/teams/delete/{teamId}")

	public ResponseEntity<DeleteTeamResponse> deleteTeam(@PathVariable("version")
	String version, @PathVariable("teamId")
	Long teamId, @RequestHeader("x-trace-id")
	String traceId, @RequestHeader("x-tenant-id")
	String tenantId, @RequestHeader(name = "x-correlation-id", required = false)
	String correlationId, @RequestHeader(name = "language-id", required = false)
	String languageId, @RequestHeader(name = "authorization", required = true)
	String authorization, @AuthenticationPrincipal
	UserDetailsImpl userDetails) {

		log.info("[{}]|DELETE_TEAM|teamId={}", traceId, teamId);

		if (!validateUser(tenantId, userDetails, traceId)) {
			return buildErrorResponse(traceId, ResponseCode.USMG_401, "Unauthorized", "User's organization does not match tenant ID",
					HttpStatus.UNAUTHORIZED, new DeleteTeamResponse());
		}

		try {
			return teamService.deleteTeam(tenantId, teamId, traceId);
		}
		catch (Exception e) {
			log.error("[{}]|DELETE_TEAM|EXCEPTION|{}", traceId, e.getMessage(), e);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Internal server error", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR,
					new DeleteTeamResponse());
		}
	}

	@PostMapping("/teams/toggle/leader")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> toggleLeader(@PathVariable("version")
	String version, @RequestHeader("x-trace-id")
	String xTraceId, @RequestHeader("x-tenant-id")
	String tenantId, @RequestHeader(name = "x-correlation-id", required = false)
	String correlationId, @RequestHeader(name = "language-id", required = false)
	String languageId, @AuthenticationPrincipal
	UserDetailsImpl userDetails, @RequestBody
	ToggleUser togleUser) {
		try {
			BaseResponse response = teamService.toggleUser(xTraceId, tenantId, userDetails, togleUser);
			response.setTimestamp(new Date());
			return ResponseEntity.status(resolveHttpStatus(response.getCode())).body(response);
		}
		catch (Exception e) {
			log.error("[{}]|auth|sso-providers|exception|error:{}", xTraceId, e.getMessage(), e);
			return buildErrorResponse(xTraceId, ResponseCode.USMG_500, "Internal server error", e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR,
					null);
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

	private <T extends BaseResponse> ResponseEntity<T> buildErrorResponse(String traceId, String code, String message, String devMessage,
			HttpStatus status, T response) {

		log.warn("[{}]|ERROR_RESPONSE|code={} message={}", traceId, code, message);

		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setTimestamp(new Date());
		response.setErrors(List.of(new ErrorObj(null, code, message, devMessage)));

		return new ResponseEntity<>(response, status);
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
