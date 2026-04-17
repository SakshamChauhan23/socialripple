package com.social.ripple.usermanagement.service.implementation;

import java.time.LocalDateTime;
import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dao.model.Team;
import com.social.ripple.usermanagement.dao.model.User;
import com.social.ripple.usermanagement.dao.model.UserTeam;
import com.social.ripple.usermanagement.dao.repository.TeamRepository;
import com.social.ripple.usermanagement.dao.repository.UserRepository;
import com.social.ripple.usermanagement.dao.repository.UserTeamRepository;
import com.social.ripple.usermanagement.dto.request.AddTeamMemberRequest;
import com.social.ripple.usermanagement.dto.request.RemoveTeamMemberRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.service.IUserTeamService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserTeamServiceImpl implements IUserTeamService {

	private final TeamRepository teamRepository;
	private final UserRepository userRepository;
	private final UserTeamRepository userTeamRepository;

	public UserTeamServiceImpl(TeamRepository teamRepository, UserRepository userRepository,
			UserTeamRepository userTeamRepository) {
		this.teamRepository = teamRepository;
		this.userRepository = userRepository;
		this.userTeamRepository = userTeamRepository;
	}

	@Override
	public ResponseEntity<BaseResponse> addMembersToTeam(Long teamId, String tenantId, AddTeamMemberRequest request,
			String traceId) {
		log.info("[{}]|ADD_TEAM_MEMBERS|STARTED|teamId={}", traceId, teamId);

		String validationError = validateAddMemberRequest(teamId, tenantId, request);
		if (validationError != null) {
			return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request", validationError,
					HttpStatus.BAD_REQUEST);
		}

		Team team = teamRepository.findById(teamId).orElse(null);
		if (team == null) {
			return buildErrorResponse(traceId, ResponseCode.USMG_404, "Team not found", "Team ID does not exist",
					HttpStatus.NOT_FOUND);
		}

		Long tenantOrgId = Long.parseLong(tenantId);
		if (!Objects.equals(team.getCreatedBy().getOrganization().getId(), tenantOrgId)) {
			return buildErrorResponse(traceId, ResponseCode.USMG_403, "Forbidden",
					"Team does not belong to this tenant", HttpStatus.FORBIDDEN);
		}

		List<UserTeam> toSave = new ArrayList<>();
		List<Long> invalidUserIds = new ArrayList<>();

		for (Long userId : request.getUserIds()) {
			User user = userRepository.findById(userId).orElse(null);
			if (user == null || !Objects.equals(user.getOrganization().getId(), tenantOrgId)) {
				invalidUserIds.add(userId);
				continue;
			}

			if (userTeamRepository.findByTeamIdAndUserId(teamId, userId).isPresent()) {
				continue; // Already exists
			}

			UserTeam userTeam = new UserTeam();
			userTeam.setTeam(team);
			userTeam.setUser(user);
			userTeam.setJoinedAt(LocalDateTime.now());
			userTeam.setActive(true);
			userTeam.setRole("MEMBER");

			toSave.add(userTeam);
		}

		if (!invalidUserIds.isEmpty()) {
			log.warn("[{}]|ADD_TEAM_MEMBERS|INVALID_USER_IDS|{}", traceId, invalidUserIds);
			return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid users",
					"User(s) not found or don't belong to this tenant: " + invalidUserIds, HttpStatus.BAD_REQUEST);
		}

		if (toSave.isEmpty()) {
			log.warn("[{}]|ADD_TEAM_MEMBERS|NO_VALID_MEMBERS_ADDED", traceId);
			return buildErrorResponse(traceId, ResponseCode.USMG_204, "No users added",
					"No new users added (maybe already members)", HttpStatus.OK);
		}

		userTeamRepository.saveAll(toSave);
		log.info("[{}]|ADD_TEAM_MEMBERS|SUCCESS|Added={}", traceId, toSave.size());
		return buildSuccessResponse("Team members added successfully");
	}

	@Override
	public ResponseEntity<BaseResponse> removeMemberFromTeam(RemoveTeamMemberRequest request, String tenantId,
			String traceId) {
		Long teamId = request.getTeamId();
		Long userId = request.getUserId();

		log.info("[{}]|REMOVE_TEAM_MEMBER|STARTED|teamId={} userId={}", traceId, teamId, userId);

		if (teamId == null || userId == null || !isValidTenantId(tenantId)) {
			return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request",
					"Missing teamId, userId or tenantId", HttpStatus.BAD_REQUEST);
		}

		Optional<UserTeam> userTeamOpt = userTeamRepository.findByTeamIdAndUserId(teamId, userId);
		if (userTeamOpt.isEmpty()) {
			log.warn("[{}]|REMOVE_TEAM_MEMBER|NOT_FOUND|userId={}", traceId, userId);
			return buildErrorResponse(traceId, ResponseCode.USMG_404, "Mapping not found",
					"User is not a member of this team", HttpStatus.NOT_FOUND);
		}

		userTeamRepository.delete(userTeamOpt.get());
		log.info("[{}]|REMOVE_TEAM_MEMBER|SUCCESS|userId={} teamId={}", traceId, userId, teamId);
		return buildSuccessResponse("Team member removed successfully");
	}

	private String validateAddMemberRequest(Long teamId, String tenantId, AddTeamMemberRequest request) {
		if (teamId == null)
			return "Team ID is required";
		if (!isValidTenantId(tenantId))
			return "Invalid tenant ID";
		if (request == null || request.getUserIds() == null || request.getUserIds().isEmpty()) {
			return "At least one user ID must be provided";
		}
		return null;
	}

	private boolean isValidTenantId(String tenantId) {
		try {
			return tenantId != null && Long.parseLong(tenantId) > 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private ResponseEntity<BaseResponse> buildSuccessResponse(String message) {
		BaseResponse response = new BaseResponse();
		response.setStatus(true);
		response.setCode(ResponseCode.USMG_200);
		response.setMessage(message);
		response.setTimestamp(new Date());
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	private ResponseEntity<BaseResponse> buildErrorResponse(String traceId, String code, String message,
			String devMessage, HttpStatus status) {
		log.warn("[{}]|ERROR|{}|{}", traceId, code, message);
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
