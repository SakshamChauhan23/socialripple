/**
 * Filename: ActivityController.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
 * property rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this
 * software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements
 * explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use this software
 * internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures, routines,
 * customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted above, no
 * license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the license
 * granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any copies. This
 * software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public performance or
 * display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly prohibited and may
 * be in violation of applicable laws.
 */
package com.social.ripple.usermanagement.controller;

import com.social.ripple.usermanagement.dto.response.ActivityShareResponse;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.DashboardResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IActivityService;
import com.social.ripple.usermanagement.service.IDashboardService;
import com.social.ripple.usermanagement.service.ITimelineService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("{version}/api/dashboard")
public class DashboardController {

	@Autowired
	private IDashboardService dashboardService;

	@GetMapping("employee/engagement-metrics")
	public ResponseEntity<BaseResponse> fetchEmployeeEngagementMetrics(@PathVariable("version")
																  String version,@AuthenticationPrincipal
																  UserDetailsImpl userDetails, @RequestHeader(value = "x-trace-id", required = true)
																  String traceId, @RequestHeader(value = "x-tenant-id", required = true)
																  String tenantId) {
		try {
			DashboardResponse response = dashboardService.fetchEmployeeEngagementMetrics(traceId, tenantId, userDetails);
			response.setTimestamp(new Date());
			response.setStatus(true);

			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			return buildErrorResponse("activities/posts", ResponseCode.USMG_500, "Internal server error", e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("employee/monthly-participation")
	public ResponseEntity<BaseResponse> fetchEmployeeMonthlyParticipation(@PathVariable("version")
																	   String version,@AuthenticationPrincipal
																	   UserDetailsImpl userDetails, @RequestHeader(value = "x-trace-id", required = true)
																	   String traceId, @RequestHeader(value = "x-tenant-id", required = true)
																	   String tenantId) {
		try {
			DashboardResponse response = dashboardService.fetchEmployeeMonthlyParticipation(traceId, tenantId, userDetails);
			response.setTimestamp(new Date());
			response.setStatus(true);

			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			return buildErrorResponse("activities/posts", ResponseCode.USMG_500, "Internal server error", e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("employee/top-contributors")
	public ResponseEntity<BaseResponse> fetchTopContributors(@PathVariable("version")
																		  String version,@AuthenticationPrincipal
																		  UserDetailsImpl userDetails, @RequestHeader(value = "x-trace-id", required = true)
																		  String traceId, @RequestHeader(value = "x-tenant-id", required = true)
																		  String tenantId) {
		try {
			DashboardResponse response = dashboardService.fetchTopContributors(traceId, tenantId, userDetails);
			response.setTimestamp(new Date());
			response.setStatus(true);

			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			return buildErrorResponse("activities/posts", ResponseCode.USMG_500, "Internal server error", e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private ResponseEntity<BaseResponse> buildErrorResponse(String path, String code, String error, String message, HttpStatus status) {
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
}
