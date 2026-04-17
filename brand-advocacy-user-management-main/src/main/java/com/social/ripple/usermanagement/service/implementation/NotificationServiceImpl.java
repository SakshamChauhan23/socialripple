/**
 * Filename: INotificationServiceImpl.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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
package com.social.ripple.usermanagement.service.implementation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dao.model.Notification;
import com.social.ripple.usermanagement.dao.repository.NotificationRepository;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.UserNotificationResponse;
import com.social.ripple.usermanagement.dto.response.NotificationResponseDto;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.INotificationService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class NotificationServiceImpl implements INotificationService {

	private final NotificationRepository notificationRepository;
	public NotificationServiceImpl(NotificationRepository notificationRepository) {
		this.notificationRepository = notificationRepository;
	}

	@Override
	public UserNotificationResponse fetchUserNotifications(String traceId, String tenantId, UserDetailsImpl userDetails, int page, int size) {
		Date timestamp = new Date();
		log.info("[{}]|NOTIFICATIONS|FETCH_INIT|Fetching notifications", traceId);

		UserNotificationResponse response = new UserNotificationResponse();
		try {
			if (userDetails == null) {
				log.warn("[{}]|NOTIFICATIONS|UNAUTHORIZED|Missing user details", traceId);
				response.setStatus(false);
				response.setCode(ResponseCode.USMG_401);
				response.setMessage("Unauthorized");
				response.setDevMessage("UserDetails or userId is null");
				response.setTimestamp(timestamp);
				response.setNotificationData(Collections.emptyList());

				ErrorObj errorObj = new ErrorObj("notifications", ResponseCode.USMG_401, "Unauthorized", "userId is null");
				response.setErrors(List.of(errorObj));
				return response;
			}
			Long userOrgId = (userDetails.getOrganization() != null) ? userDetails.getOrganization().getId() : null;

			if (tenantId == null || tenantId.isBlank() || userOrgId == null || !tenantId.equals(String.valueOf(userOrgId))) {
				log.warn("[{}]|NOTIFICATIONS|UNAUTHORIZED|TenantId mismatch", traceId);
				response.setStatus(false);
				response.setCode(ResponseCode.USMG_401);
				response.setMessage("Unauthorized");
				response.setDevMessage("Invalid or mismatched tenantId");
				response.setTimestamp(timestamp);
				response.setNotificationData(Collections.emptyList());

				ErrorObj errorObj = new ErrorObj("notifications", ResponseCode.USMG_401, "Unauthorized", "Invalid or mismatched tenantId");
				response.setErrors(List.of(errorObj));
				return response;
			}
			Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

			Page<Notification> notificationPage = notificationRepository.findByUserId(userDetails.getUserId(), pageable);

			List<NotificationResponseDto> dtoList = new ArrayList<>();
			for (Notification n : notificationPage.getContent()) {
				if (n != null && n.getId() != null) {
					NotificationResponseDto dto = new NotificationResponseDto();
					dto.setUserId(n.getUserId());
					dto.setType(n.getType());
					dto.setData(n.getData());
					dto.setRead(n.getIsRead());
					dto.setCreatedAt(n.getCreatedAt());
					dtoList.add(dto);
				}
			}
			log.info("[{}]|NOTIFICATIONS|SUCCESS|Fetched {} notifications", traceId, dtoList.size());

			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("Notifications fetched successfully");
			response.setDevMessage("User's notifications retrieved");
			response.setTimestamp(timestamp);
			response.setNotificationData(dtoList);
			return response;

		}
		catch (Exception ex) {
			log.error("[{}]|NOTIFICATIONS|EXCEPTION|{}", traceId, ex.getMessage(), ex);

			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal server error");
			response.setDevMessage(ex.getMessage());
			response.setTimestamp(timestamp);
			response.setNotificationData(Collections.emptyList());

			ErrorObj errorObj = new ErrorObj("notifications", ResponseCode.USMG_500, "Internal server error", ex.getMessage());
			response.setErrors(List.of(errorObj));
			return response;
		}
	}
}