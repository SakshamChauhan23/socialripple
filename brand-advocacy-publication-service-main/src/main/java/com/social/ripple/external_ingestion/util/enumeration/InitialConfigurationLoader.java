/**
 * Filename: InitialConfigurationLoader.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all
 * intellectual property rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix.
 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the
 * license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies. This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not
 * publicly available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public
 * performance or display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly
 * prohibited and may be in violation of applicable laws.
 */
package com.social.ripple.external_ingestion.util.enumeration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.social.ripple.external_ingestion.constants.ConfigKeys;
import com.social.ripple.external_ingestion.dao.model.ConfigParameter;
import com.social.ripple.external_ingestion.dao.repository.ConfigParameterRepository;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class InitialConfigurationLoader {

    private final ConfigParameterRepository configParametersRepo;
    private final AppCache cache;

    public InitialConfigurationLoader(ConfigParameterRepository configParametersRepo, AppCache cache) {
        this.configParametersRepo = configParametersRepo;
        this.cache = cache;
    }

    @PostConstruct
    public void loadCache() {
        String traceId = "INIT";
        log.info("[{}]|CONFIG|INIT|Starting cache load", traceId);

        try {
            List<ConfigParameter> allParams = configParametersRepo.findAll();
            Map<String, ConfigParameter> paramMap = allParams.stream()
                    .collect(Collectors.toMap(ConfigParameter::getConfigKey, cp -> cp));

            Set<String> missingKeys = ConfigKeys.REQUIRED_KEYS.stream()
                    .filter(key -> !paramMap.containsKey(key))
                    .collect(Collectors.toSet());

            if (!missingKeys.isEmpty()) {
                throw new IllegalStateException(
                        "Missing required config keys: " + String.join(", ", missingKeys));
            }

            cache.setConfigParameters(traceId, paramMap);
            log.info("[{}]|CONFIG|INIT|Cache load completed successfully", traceId);
        } catch (Exception e) {
            log.error("[{}]|CONFIG|INIT|Cache load failed: {}", traceId, e.getMessage(), e);
            throw new RuntimeException("Application startup aborted due to config errors", e);
        }
    }
}
