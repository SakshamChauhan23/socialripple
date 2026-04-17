package com.social.ripple.usermanagement.service;

import org.springframework.data.domain.Pageable;

import org.springframework.http.ResponseEntity;

import com.social.ripple.usermanagement.dto.request.UpdateUserStatusRequest;
import com.social.ripple.usermanagement.dto.response.GetAllUsersResponse;
import com.social.ripple.usermanagement.dto.response.UpdateUserStatusResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;

public interface IUserService {

	ResponseEntity<GetAllUsersResponse> getUsersByOrganization(String traceId, Long organizationId, String tenantId,
			UserDetailsImpl userDetails, Pageable pageable);

	ResponseEntity<UpdateUserStatusResponse> ToggleUserStatus(String traceId, Long userId,
			UpdateUserStatusRequest request, String tenantId, UserDetailsImpl userDetails);

	ResponseEntity<UpdateUserStatusResponse> removeUser(String traceId, Long userId, String tenantId, UserDetailsImpl userDetails);
}
