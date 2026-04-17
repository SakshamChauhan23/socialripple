/**
 * Filename: AppCache.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property rights)
 * in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this software is
 * forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements explicitly
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
package com.social.ripple.notification_service.util.constants;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.social.ripple.notification_service.dao.model.ConfigParameter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AppCache {

	public static final Map<String, ConfigParameter> configParameters = new ConcurrentHashMap<>();

	public AppCache() {
		log.info("[{}]|Cache|Initialization|AppCache instance created.", "SYSTEM");
	}

	public void setConfigParameters(String traceId, Map<String, ConfigParameter> parameters) {
		try {
			if (parameters != null) {
				log.info("[{}]|Cache|Create|Received {} ConfigParameters.", traceId, parameters.size());
				configParameters.clear();
				configParameters.putAll(parameters);
				log.info("[{}]|Cache|Create|Success|ConfigParameters cache updated.", traceId);
			}
			else {
				log.warn("[{}]|Cache|Create|Skipped|Null parameter map provided.", traceId);
			}
		}
		catch (Exception ex) {
			log.error("[{}]|Cache|Create|Exception|Error while setting ConfigParameters: {}", traceId, ex.getMessage(), ex);
		}
	}
}
