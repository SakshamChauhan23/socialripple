/**
 * Filename: InitialConfigurationLoader.java
 *
 * © Copyright 2024 Quasarix. ALL RIGHTS RESERVED.

 * All rights, title and interest (including all intellectual property rights) in this software and any derivative works based upon or derived from
 * this software belongs exclusively to Quasarix.

 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment,
 * the license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies.

 * This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix.

 * Any use, reproduction, modification, distribution, public performance or display of this software or through the use of this software without the
 * prior, express written consent of Quasarix is strictly prohibited and may be in violation of applicable laws.
 *
 */
package com.social.ripple.notification_service.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.social.ripple.notification_service.dao.model.ConfigParameter;
import com.social.ripple.notification_service.dao.repository.ConfigParameterRepository;
import com.social.ripple.notification_service.util.constants.AppCache;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InitialConfigurationLoader implements ApplicationContextAware {

	private final ConfigParameterRepository configParametersRepo;
	private final AppCache cache;
	private final AppCommonValidator appCommonValidator;
	private ApplicationContext applicationContext;

	public InitialConfigurationLoader(ConfigParameterRepository configParametersRepo, AppCache cache, AppCommonValidator appCommonValidator) {
		this.configParametersRepo = configParametersRepo;
		this.cache = cache;
		this.appCommonValidator = appCommonValidator;
	}
 
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@PostConstruct
	public void reloadCache() {
		String traceId = appCommonValidator.getOrGenerateRequestId((String) null);
		log.info("[{}]|CONFIG|INIT|Starting cache load during application startup", traceId);

		try {
			loadConfigurationsIntoAppCache(traceId);
			log.info("[{}]|CONFIG|INIT|Cache load completed successfully", traceId);
		}
		catch (Exception e) {
			log.error("[{}]|CONFIG|INIT|Cache load failed with error: {}", traceId, e.getMessage(), e);
		}
	}

	public void loadConfigurationsIntoAppCache(String traceId) {

		log.info("[{}]|CONFIGURATION|loadConfigurationParameters|Start loading configuration parameters", traceId);

		try {
			Map<String, ConfigParameter> configParameterMap = new HashMap<>();
			List<ConfigParameter> configParameterValues = configParametersRepo.findAll();
			log.info("[{}]|CONFIGURATION|loadConfigurationParameters|Fetched config parameters from database", traceId);

			for (ConfigParameter val : configParameterValues) {
				configParameterMap.put(val.getConfigKey(), val);
			}

			Set<String> missingKeys = getMissingRequiredParameters(traceId, configParameterMap.keySet());
			if (!missingKeys.isEmpty()) {
				String reason = "Missing Required Configuration Keys: " + String.join(", ", missingKeys);
				gracefullyShutdown(traceId, reason);
				return;
			}

			cache.setConfigParameters("", configParameterMap);
			log.info("[{}]|CONFIGURATION|loadConfigurationParameters|Configuration Parameters loaded into cache", traceId);

		}
		catch (Exception e) {
			log.error("[{}]|CONFIGURATION|loadConfigurationParameters|Exception occurred while loading configuration parameters: {}", traceId,
					e.getMessage(), e);
			gracefullyShutdown(traceId, "Application shutdown due to error during configuration loading.");
		}
	}

	private Set<String> getMissingRequiredParameters(String traceId, Set<String> availableKeys) {
		Set<String> missing = ConfigKeys.REQUIRED_KEYS.stream()
				.filter(requiredKey -> !availableKeys.contains(requiredKey))
				.collect(Collectors.toSet());

		if (!missing.isEmpty()) {
			log.warn("[{}]|CONFIGURATION|getMissingRequiredParameters|Missing keys: {}", traceId, missing);
		}
		else {
			log.info("[{}]|CONFIGURATION|getMissingRequiredParameters|No required keys are missing", traceId);
		}
		return missing;
	}

	private void gracefullyShutdown(String traceId, String reason) {
		log.error("[{}]|CONFIGURATION|GracefulShutdown|Triggered: {}", traceId, reason);
		if (applicationContext instanceof ConfigurableApplicationContext context) {
			try {
				context.close();
				log.info("[{}]|CONFIGURATION|GracefulShutdown|ApplicationContext closed", traceId);
			}
			catch (Exception e) {
				log.error("[{}]|CONFIGURATION|GracefulShutdown|Failed to close ApplicationContext: {}", traceId, e.getMessage(), e);
				System.exit(1);
			}
		}
		else {
			log.error("[{}]|CONFIGURATION|GracefulShutdown|Unable to cast ApplicationContext to ConfigurableApplicationContext", traceId);
			System.exit(1);
		}
	}
}


