package com.social.ripple.usermanagement.service;

import com.social.ripple.usermanagement.dto.request.CreateTeamRequest;
import com.social.ripple.usermanagement.dto.request.ToggleUser;
import com.social.ripple.usermanagement.dto.request.UpdateTeamRequest;
import com.social.ripple.usermanagement.dto.response.*;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import org.springframework.http.ResponseEntity;

public interface IDashboardService {

	DashboardResponse fetchEmployeeEngagementMetrics(String traceId, String tenantId,UserDetailsImpl userDetails);

	DashboardResponse fetchEmployeeMonthlyParticipation(String traceId, String tenantId, UserDetailsImpl userDetails);

	DashboardResponse fetchTopContributors(String traceId, String tenantId, UserDetailsImpl userDetails);
}
