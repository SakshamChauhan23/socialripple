package com.social.ripple.usermanagement.service.implementation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.social.ripple.usermanagement.dao.model.Team;
import com.social.ripple.usermanagement.dao.model.User;
import com.social.ripple.usermanagement.dao.model.UserTeam;
import com.social.ripple.usermanagement.dao.repository.TeamRepository;
import com.social.ripple.usermanagement.dao.repository.UserRepository;
import com.social.ripple.usermanagement.dao.repository.UserTeamRepository;
import com.social.ripple.usermanagement.dto.request.CreateTeamRequest;
import com.social.ripple.usermanagement.dto.request.ToggleUser;
import com.social.ripple.usermanagement.dto.request.UpdateTeamRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.CreateTeamResponse;
import com.social.ripple.usermanagement.dto.response.DeleteTeamResponse;
import com.social.ripple.usermanagement.dto.response.GetAllTeamsResponse;
import com.social.ripple.usermanagement.dto.response.GetTeamDetailsResponse;
import com.social.ripple.usermanagement.dto.response.TeamMemberDto;
import com.social.ripple.usermanagement.dto.response.TeamSummary;
import com.social.ripple.usermanagement.dto.response.UpdateTeamResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.ITeamService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class TeamServiceImpl implements ITeamService {

	private final TeamRepository teamRepository;
	private final UserRepository userRepository;
	private final UserTeamRepository userTeamRepository;
	private final RestTemplate restTemplate;

	public TeamServiceImpl(TeamRepository teamRepository, UserRepository userRepository,
			UserTeamRepository userTeamRepository, RestTemplate restTemplate) {
		this.teamRepository = teamRepository;
		this.userRepository = userRepository;
		this.userTeamRepository = userTeamRepository;
		this.restTemplate = restTemplate;
	}

	@Override
	public ResponseEntity<CreateTeamResponse> createTeam(CreateTeamRequest request, String tenantId, String userId,
			String traceId) {
		log.info("[{}]|CREATE_TEAM|STARTED", traceId);

		if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(userId) || request == null
				|| !StringUtils.hasText(request.getTeamName())) {
			return buildErrorResponse(new CreateTeamResponse(), "USMG_400", "Missing required fields",
					"TenantId/UserId/TeamName required", HttpStatus.BAD_REQUEST);
		}

		try {
			Long creatorId = Long.parseLong(userId);
			Optional<User> userOpt = userRepository.findById(creatorId);

			if (userOpt.isEmpty()) {
				return buildErrorResponse(new CreateTeamResponse(), "USMG_404", "User not found", "Invalid user ID",
						HttpStatus.NOT_FOUND);
			}

			String trimmedName = request.getTeamName().trim();
			if (teamRepository.existsByTeamName(trimmedName)) {
				return buildErrorResponse(new CreateTeamResponse(), "USMG_409", "Team name already exists",
						"Duplicate team name", HttpStatus.CONFLICT);
			}

			Team team = new Team();
			team.setTeamName(trimmedName);
			team.setCreatedBy(userOpt.get());
			team.setCreatedAt(LocalDateTime.now());
			team.setImageUrl(request.getImageUrl());
			team = teamRepository.save(team);

			log.info("[{}]|CREATE_TEAM|SUCCESS|teamId={}", traceId, team.getId());

			CreateTeamResponse response = new CreateTeamResponse();
			response.setStatus(true);
			response.setCode("USMG_200");
			response.setMessage("Team created successfully");
			response.setTimestamp(new Date());
			response.setTeamId(team.getId());
			response.setTeamName(team.getTeamName());
			response.setImageCode(team.getImageUrl());
			return ResponseEntity.ok(response);

		} catch (NumberFormatException ex) {
			return buildErrorResponse(new CreateTeamResponse(), "USMG_400", "Invalid format", "UserId must be numeric",
					HttpStatus.BAD_REQUEST);
		} catch (Exception ex) {
			log.error("[{}]|CREATE_TEAM|ERROR={}", traceId, ex.getMessage(), ex);
			return buildErrorResponse(new CreateTeamResponse(), "USMG_500", "Internal server error", ex.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<GetAllTeamsResponse> getAllTeams(String tenantId, String traceId) {
		log.info("[{}]|GET_ALL_TEAMS|STARTED", traceId);

		try {
			List<Team> teams = teamRepository.findByCreatedByOrganizationId(Long.parseLong(tenantId));

			List<TeamSummary> summaries = new ArrayList<>();
			for (Team team : teams) {
				TeamSummary summary = new TeamSummary();
				summary.setTeamId(team.getId());
				summary.setTeamName(team.getTeamName());
				summary.setImageUrl(team.getImageUrl());
				summaries.add(summary);
			}

			GetAllTeamsResponse response = new GetAllTeamsResponse();
			response.setStatus(true);
			response.setCode("USMG_200");
			response.setMessage("Teams fetched successfully");
			response.setTimestamp(new Date());
			response.setTeams(summaries);

			log.info("[{}]|GET_ALL_TEAMS|SUCCESS|count={}", traceId, summaries.size());
			return ResponseEntity.ok(response);

		} catch (Exception ex) {
			log.error("[{}]|GET_ALL_TEAMS|ERROR={}", traceId, ex.getMessage(), ex);
			return buildErrorResponse(new GetAllTeamsResponse(), "USMG_500", "Internal error", ex.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<GetTeamDetailsResponse> getTeamDetailsById(String tenantId, Long teamId, String traceId) {
		log.info("[{}]|GET_TEAM_DETAILS|STARTED|teamId={}", traceId, teamId);

		if (!StringUtils.hasText(tenantId) || teamId == null) {
			return buildErrorResponse(new GetTeamDetailsResponse(), "USMG_400", "Missing fields",
					"TenantId and TeamId are required", HttpStatus.BAD_REQUEST);
		}

		try {
			Long.parseLong(tenantId);

			Optional<Team> teamOpt = teamRepository.findById(teamId);
			if (teamOpt.isEmpty()) {
				return buildErrorResponse(new GetTeamDetailsResponse(), "USMG_404", "Team not found", "Invalid team ID",
						HttpStatus.NOT_FOUND);
			}

			Team team = teamOpt.get();
			List<UserTeam> userTeams = userTeamRepository.findByTeam(team);

			List<TeamMemberDto> members = new ArrayList<>();
			for (UserTeam ut : userTeams) {
				User u = ut.getUser();
				members.add(new TeamMemberDto(u.getId(), u.getName(), u.getEmail(), ut.getRole(), ut.isActive(),
						ut.getJoinedAt()));
			}

			GetTeamDetailsResponse response = new GetTeamDetailsResponse();
			response.setStatus(true);
			response.setCode("USMG_200");
			response.setMessage("Team fetched successfully");
			response.setTimestamp(new Date());
			response.setTeamId(team.getId());
			response.setTeamName(team.getTeamName());
			response.setCreatedBy(team.getCreatedBy().getName());
			response.setCreatedAt(team.getCreatedAt());
			response.setMembers(members);

			log.info("[{}]|GET_TEAM_DETAILS|SUCCESS|teamId={}|members={}", traceId, teamId, members.size());
			return ResponseEntity.ok(response);

		} catch (NumberFormatException ex) {
			return buildErrorResponse(new GetTeamDetailsResponse(), "USMG_400", "Invalid tenant ID",
					"Tenant ID must be numeric", HttpStatus.BAD_REQUEST);
		} catch (Exception ex) {
			log.error("[{}]|GET_TEAM_DETAILS|ERROR={}", traceId, ex.getMessage(), ex);
			return buildErrorResponse(new GetTeamDetailsResponse(), "USMG_500", "Internal server error",
					ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<UpdateTeamResponse> updateTeam(UpdateTeamRequest request, String tenantId, Long teamId,
			String traceId) {
		log.info("[{}]|UPDATE_TEAM|STARTED|teamId={}", traceId, teamId);

		if (!StringUtils.hasText(tenantId) || teamId == null || request == null
				|| !StringUtils.hasText(request.getNewTeamName())) {
			return buildErrorResponse(new UpdateTeamResponse(), "USMG_400", "Missing fields",
					"TenantId, TeamId, and NewTeamName are required", HttpStatus.BAD_REQUEST);
		}

		try {
			Optional<Team> teamOpt = teamRepository.findById(teamId);

			if (teamOpt.isEmpty()) {
				return buildErrorResponse(new UpdateTeamResponse(), "USMG_404", "Team not found", "Invalid team ID",
						HttpStatus.NOT_FOUND);
			}

			Team team = teamOpt.get();
			String newName = request.getNewTeamName().trim();

			if (!team.getTeamName().equalsIgnoreCase(newName) && teamRepository.existsByTeamName(newName)) {
				return buildErrorResponse(new UpdateTeamResponse(), "USMG_409", "Team name already exists",
						"Another team with the same name already exists", HttpStatus.CONFLICT);
			}

			team.setTeamName(newName);
			if (StringUtils.hasText(request.getImageUrl())) {
				team.setImageUrl(request.getImageUrl().trim());
			}

			teamRepository.save(team);

			UpdateTeamResponse response = new UpdateTeamResponse();
			response.setStatus(true);
			response.setCode("USMG_200");
			response.setMessage("Team updated successfully");
			response.setTimestamp(new Date());
			response.setTeamId(team.getId());
			response.setNewTeamName(team.getTeamName());
			response.setImageUrl(team.getImageUrl());

			log.info("[{}]|UPDATE_TEAM|SUCCESS|teamId={}", traceId, team.getId());
			return ResponseEntity.ok(response);

		} catch (Exception ex) {
			log.error("[{}]|UPDATE_TEAM|ERROR={}", traceId, ex.getMessage(), ex);
			return buildErrorResponse(new UpdateTeamResponse(), "USMG_500", "Internal server error", ex.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@Override
	public ResponseEntity<DeleteTeamResponse> deleteTeam(String tenantId, Long teamId, String traceId) {
		log.info("[{}]|DELETE_TEAM|STARTED|teamId={}", traceId, teamId);

		if (!StringUtils.hasText(tenantId) || teamId == null) {
			return buildErrorResponse(new DeleteTeamResponse(), "USMG_400", "Missing fields",
					"TenantId and TeamId are required", HttpStatus.BAD_REQUEST);
		}

		try {
			Optional<Team> teamOpt = teamRepository.findById(teamId);

			if (teamOpt.isEmpty()) {
				return buildErrorResponse(new DeleteTeamResponse(), "USMG_404", "Team not found",
						"No team found with the given ID", HttpStatus.NOT_FOUND);
			}

			teamRepository.delete(teamOpt.get());

			DeleteTeamResponse response = new DeleteTeamResponse();
			response.setStatus(true);
			response.setCode("USMG_200");
			response.setMessage("Team deleted successfully");
			response.setTimestamp(new Date());

			log.info("[{}]|DELETE_TEAM|SUCCESS|teamId={}", traceId, teamId);
			return ResponseEntity.ok(response);

		} catch (Exception ex) {
			log.error("[{}]|DELETE_TEAM|ERROR={}", traceId, ex.getMessage(), ex);
			return buildErrorResponse(new DeleteTeamResponse(), "USMG_500", "Internal server error", ex.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private <T extends BaseResponse> ResponseEntity<T> buildErrorResponse(T response, String code, String message,
			String devMessage, HttpStatus status) {
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setTimestamp(new Date());
		return new ResponseEntity<>(response, status);
	}

	@Override
	public BaseResponse toggleUser(String traceId, String tenantId, UserDetailsImpl userDetails,
			ToggleUser toggleUser) {

		log.info("[{}]|USMG|ToggleUser|Start|userId:{}|tenantId:{}|isLeader:{}", traceId, toggleUser.getUserId(),
				tenantId, toggleUser.getIsLeader());

		BaseResponse response = new BaseResponse();

		Long userId = toggleUser.getUserId();
		Boolean isLeader = toggleUser.getIsLeader();

		if (userId == null) {
			log.warn("[{}]|USMG|ToggleUser|ValidationFailed|UserId is null", traceId);
			response.setStatus(false);
			response.setCode("USMG_400");
			response.setMessage("UserId is required");
			response.setTimestamp(new Date());
			return response;
		}

		try {
			Optional<User> userOpt = userRepository.findById(userId);
			if (userOpt.isEmpty()) {
				response.setStatus(false);
				response.setCode("USMG_404");
				response.setMessage("User not found with id: " + userId);
				response.setTimestamp(new Date());
				return response;
			}

			User user = userOpt.get();

			if (!isUserBelongsToSameOrg(traceId, userDetails, user)) {
				response.setStatus(false);
				response.setCode("USMG_403");
				response.setMessage("Requester and user do not belong to same organization");
				response.setTimestamp(new Date());
				return response;
			}

			user.setIsLeader(isLeader);
			userRepository.save(user);

			if (Boolean.TRUE.equals(isLeader)) {
				triggerLeaderSync(traceId);
			}

			response.setStatus(true);
			response.setCode("USMG_200");
			response.setMessage(Boolean.TRUE.equals(isLeader) ? "User marked as leader successfully"
					: "User removed from leader role successfully");
			response.setTimestamp(new Date());
			return response;

		} catch (Exception e) {
			log.error("[{}]|USMG|ToggleUser|Error|{}", traceId, e.getMessage(), e);
			response.setStatus(false);
			response.setCode("USMG_500");
			response.setMessage("Failed to update user leader status");
			response.setTimestamp(new Date());
			return response;
		}
	}

	private boolean isUserBelongsToSameOrg(String traceId, UserDetailsImpl userDetails, User user) {
		try {
			if (user == null || userDetails == null) {
				log.warn("[{}]|USER|VALIDATION_FAILED|User or userDetails is null", traceId);
				return false;
			}

			if (user.getOrganization() == null || userDetails.getOrganization() == null) {
				log.warn("[{}]|USER|VALIDATION_FAILED|Organization details are missing", traceId);
				return false;
			}

			boolean sameOrg = user.getOrganization().getId().equals(userDetails.getOrganization().getId());
			if (!sameOrg) {
				log.warn("[{}]|USER|ORG_MISMATCH|User Org: {}, Requestor Org: {}", traceId, user.getOrganization(),
						userDetails.getOrganization());
			}

			return sameOrg;
		} catch (Exception e) {
			log.error("[{}]|USER|ORG_MISMATCH|Un expected error|message : {}", e.getMessage(), e);
			return false;
		}
	}

	private void triggerLeaderSync(String traceId) {
		try {
			String pubServiceBase = "http://127.0.0.1:9080";
			String[] endpoints = {"/v1/api/x/fetch/linkedin", "/v1/api/x/fetch/facebook",
					"/v1/api/x/fetch/instagram", "/v1/api/x/fetch/x"};
			for (String endpoint : endpoints) {
				try {
					restTemplate.getForEntity(pubServiceBase + endpoint, String.class);
				} catch (Exception e) {
					log.warn("[{}]|TEAM|LeaderSync|{}|{}", traceId, endpoint, e.getMessage());
				}
			}
			log.info("[{}]|TEAM|LeaderSync|Triggered immediate sync for all platforms", traceId);
		} catch (Exception e) {
			log.warn("[{}]|TEAM|LeaderSync|Failed: {}", traceId, e.getMessage());
		}
	}

}
