package com.social.ripple.usermanagement.service;

import org.springframework.http.ResponseEntity;

import com.social.ripple.usermanagement.dto.request.CreateTeamRequest;
import com.social.ripple.usermanagement.dto.request.ToggleUser;
import com.social.ripple.usermanagement.dto.request.UpdateTeamRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.CreateTeamResponse;
import com.social.ripple.usermanagement.dto.response.DeleteTeamResponse;
import com.social.ripple.usermanagement.dto.response.GetAllTeamsResponse;
import com.social.ripple.usermanagement.dto.response.GetTeamDetailsResponse;
import com.social.ripple.usermanagement.dto.response.UpdateTeamResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;

public interface ITeamService {

	/**
	 * Creates a new team under the given tenant.
	 *
	 * @param request  the create team request containing the team name
	 * @param tenantId the organization ID from header
	 * @param userId   the ID of the admin creating the team
	 * @param traceId  unique trace ID for logging
	 * @return response entity with created team ID and name
	 */
	ResponseEntity<CreateTeamResponse> createTeam(CreateTeamRequest request, String tenantId, String userId,
			String traceId);

	/**
	 * Fetches all teams for the given tenant/organization ID.
	 *
	 * @param tenantId the ID of the tenant (organization)
	 * @param traceId  the unique trace ID for logging and debugging
	 * @return response entity containing list of teams
	 */
	ResponseEntity<GetAllTeamsResponse> getAllTeams(String tenantId, String traceId);

	/**
	 * Fetches team details including member names by team ID.
	 *
	 * @param tenantId the tenant ID to validate organization access
	 * @param teamId   the ID of the team
	 * @param traceId  the unique trace ID for logging
	 * @return response entity with team details and members
	 */
	ResponseEntity<GetTeamDetailsResponse> getTeamDetailsById(String tenantId, Long teamId, String traceId);

	/**
	 * Updates a team's name for the given teamId and organization.
	 *
	 * @param request  the update request with new team name
	 * @param tenantId the ID of the tenant (organization)
	 * @param teamId   the ID of the team to be updated
	 * @param traceId  unique trace ID for logging
	 * @return response entity with update result
	 */
	ResponseEntity<UpdateTeamResponse> updateTeam(UpdateTeamRequest request, String tenantId, Long teamId,
			String traceId);

	/**
	 * Deletes the specified team under a tenant.
	 *
	 * @param tenantId the tenant ID (organization ID)
	 * @param teamId   the team ID to be deleted
	 * @param traceId  trace ID for logging
	 * @return response entity with deletion result
	 */
	ResponseEntity<DeleteTeamResponse> deleteTeam(String tenantId, Long teamId, String traceId);

	BaseResponse toggleUser(String traceId, String tenantId, UserDetailsImpl userDetails, ToggleUser togleUser);

}
