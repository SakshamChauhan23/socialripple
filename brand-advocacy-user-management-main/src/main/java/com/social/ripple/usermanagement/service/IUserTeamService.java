package com.social.ripple.usermanagement.service;

import org.springframework.http.ResponseEntity;

import com.social.ripple.usermanagement.dto.request.AddTeamMemberRequest;
import com.social.ripple.usermanagement.dto.request.RemoveTeamMemberRequest;
import com.social.ripple.usermanagement.dto.response.BaseResponse;

public interface IUserTeamService {
	ResponseEntity<BaseResponse> addMembersToTeam(Long teamId, String tenantId, AddTeamMemberRequest request,
			String traceId);

	ResponseEntity<BaseResponse> removeMemberFromTeam(RemoveTeamMemberRequest request, String tenantId, String traceId);

}
