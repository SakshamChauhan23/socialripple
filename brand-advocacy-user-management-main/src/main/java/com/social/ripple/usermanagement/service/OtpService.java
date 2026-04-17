/**
 * Filename: OtpService.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
 * rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this software
 * is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements explicitly
 * covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use this software
 * internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures, routines,
 * customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted above, no
 * license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the license
 * granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any copies. This
 * software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public performance or
 * display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly prohibited and may
 * be in violation of applicable laws.
 */
package com.social.ripple.usermanagement.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.social.ripple.usermanagement.dao.model.OtpStore;
import com.social.ripple.usermanagement.dao.repository.OtpStoreRepository;
import com.social.ripple.usermanagement.dto.response.NotificationResponse;
import com.social.ripple.usermanagement.util.AppCache;
import com.social.ripple.usermanagement.util.AppCommonValidator;
import com.social.ripple.usermanagement.util.ConfigKeys;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OtpService {

	private final OtpStoreRepository otpStoreRepository;
	private final NotificationClient notificationClient;
	private final AppCache appCache;
	private final AppCommonValidator appCommonValidator;

	@Autowired
	public OtpService(OtpStoreRepository otpStoreRepository,
	                  NotificationClient notificationClient,
	                  AppCache appCache,
	                  AppCommonValidator appCommonValidator) {
		this.otpStoreRepository = otpStoreRepository;
		this.notificationClient = notificationClient;
		this.appCache = appCache;
		this.appCommonValidator = appCommonValidator;
	}
	@Transactional
	public boolean generateAndSendOtp(String traceId, String target, String purpose) {
		log.info("[{}]|OTP_GENERATE_SEND|START|Target: {}|Purpose: {}", traceId, target, purpose);

		try {
			int invalidatedCount = otpStoreRepository.invalidateExistingOtps(target, purpose);
			log.info("[{}]|OTP_GENERATE_SEND|INVALIDATED|Target: {}|Purpose: {}|Count: {}",
					traceId, target, purpose, invalidatedCount);

			int otpLength = Integer.parseInt(appCache.getConfigParameterValue(traceId, ConfigKeys.OTP_LENGTH) != null
					? appCache.getConfigParameterValue(traceId, ConfigKeys.OTP_LENGTH)
					: "6");

			int otpExpirationMinutes = Integer.parseInt(appCache.getConfigParameterValue(traceId, ConfigKeys.OTP_EXPIRATION_MINUTES) != null
					? appCache.getConfigParameterValue(traceId, ConfigKeys.OTP_EXPIRATION_MINUTES)
					: "15");

			String otpCode = appCommonValidator.generateNumericOtp(traceId, otpLength);

			if (otpCode == null) {
				log.error("[{}]|OTP_GENERATE_SEND|FAILED|Could not generate OTP", traceId);
				return false;
			}

			OtpStore otpStore = new OtpStore();
			otpStore.setTarget(target);
			otpStore.setOtpCode(otpCode);
			otpStore.setPurpose(purpose);
			otpStore.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes));
			otpStoreRepository.save(otpStore);

			log.info("[{}]|OTP_GENERATE_SEND|SAVED|Target: {}|Purpose: {}|Expires At: {} minutes",
					traceId, target, purpose, otpExpirationMinutes);
			NotificationResponse notificationResponse = notificationClient.sendNotification(traceId, target, otpCode);
			if (notificationResponse != null && "SUCCESS".equals(notificationResponse.getStatus())) {
				log.info("[{}]|OTP_GENERATE_SEND|NOTIFICATION_SUCCESS|Target: {}|Purpose: {}", traceId, target, purpose);
				return true;
			} else {
				log.error("[{}]|OTP_GENERATE_SEND|NOTIFICATION_FAILED|Target: {}|Purpose: {}|Message: {}",
						traceId, target, purpose,
						notificationResponse != null ? notificationResponse.getMessage() : "No response");
				return false;
			}
		} catch (Exception e) {
			log.error("[{}]|OTP_GENERATE_SEND|FAILED|Target: {}|Purpose: {}|Error: {}",
					traceId, target, purpose, e.getMessage(), e);
			return false;
		} finally {
			log.info("[{}]|OTP_GENERATE_SEND|END|Target: {}|Purpose: {}", traceId, target, purpose);
		}
	}
}
