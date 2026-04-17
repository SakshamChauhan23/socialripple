/**
 * Filename: NotificationController.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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

import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.UserNotificationResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.INotificationService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

@RestController
@RequestMapping("{version}/")
public class NotificationController {

	private final INotificationService notificationService;

	public NotificationController(INotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping("notifications")
	public ResponseEntity<BaseResponse> getNotifications(@PathVariable("version")
	String version, @AuthenticationPrincipal
	UserDetailsImpl userDetails, @RequestHeader(value = "x-trace-id", required = true)
	String traceId, @RequestHeader(value = "x-tenant-id", required = true)
	String tenantId, @RequestParam(defaultValue = "0")
	int page, @RequestParam(defaultValue = "10")
	int size) {

		try {
			UserNotificationResponse response = notificationService.fetchUserNotifications(traceId, tenantId, userDetails, page, size);
			response.setTimestamp(new Date());

			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
			return ResponseEntity.ok(response);

		}
		catch (Exception e) {
			BaseResponse response = new BaseResponse();
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal server error");
			response.setDevMessage(e.getMessage());
			response.setTimestamp(new Date());
			response.setErrors(List.of(new ErrorObj("notifications", ResponseCode.USMG_500, "Internal server error", e.getMessage())));
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}