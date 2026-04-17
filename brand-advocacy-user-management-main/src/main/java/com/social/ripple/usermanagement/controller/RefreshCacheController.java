/**
 * Filename: RefreshCacheController.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.util.InitialConfigurationLoader;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/cache")
@Slf4j
public class RefreshCacheController {

	@Autowired
	private InitialConfigurationLoader initialConfigurationLoader;

	@GetMapping("/refresh")
	public String refreshCache(@RequestHeader(name = "x-trace-id", required = true)
	String traceId) {
		log.info("START|Refreshing cache");
		try {
			initialConfigurationLoader.loadConfigurationsIntoAppCache(traceId);
			return "Cache reloaded successfully.";
		}
		catch (Exception e) {
			log.error("[{}]|EXCEPTION|RELOAD_CACHE|Message : {}",traceId,e.getMessage(),e);
			return "Unable to reload the cache"+e.getMessage();
		}

	}
}
