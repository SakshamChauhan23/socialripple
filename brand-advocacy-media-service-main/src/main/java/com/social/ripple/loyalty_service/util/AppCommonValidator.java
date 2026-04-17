/**
 * Filename: AppCommonValidator.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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
package com.social.ripple.loyalty_service.util;

import java.util.UUID;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AppCommonValidator {
	


	public boolean isNullOrEmpty(String traceId, String input, String fieldName) {
		boolean result = input == null || input.trim().isEmpty();

		if (result) {
			log.warn("[{}] Validation failed: '{}' is null or empty|input : {}", traceId, fieldName, input);
		}

		return result;
	}

	public boolean isValidEmail(String traceId, String email) {
		boolean isValid = email != null && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

		if (!isValid) {
			log.warn("[{}] Validation failed: Invalid email format: {}", traceId, email);
		}
		return isValid;
	}

	public String getOrGenerateRequestId(String requestId) {
		if (requestId != null && !requestId.isBlank()) {
			log.debug("Using provided requestId: {}", requestId);
			return requestId;
		}
		else {
			String generatedId = UUID.randomUUID().toString();
			log.info("Generated new requestId: {}", generatedId);
			return generatedId;
		}
	}

	public String getOrGenerateRequestId(HttpServletRequest request) {
		return getOrGenerateRequestId(request.getHeader("x-trace-id"));
	}
}
