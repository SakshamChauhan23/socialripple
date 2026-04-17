/**
 * Filename: TestController.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
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
package com.social.ripple.usermanagement.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dto.GoogleUserDto;
import com.social.ripple.usermanagement.dto.MicrosoftUserDto;
import com.social.ripple.usermanagement.service.ISsoTokenValidatorService;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@Slf4j
public class TestController {

	@Autowired
	private ISsoTokenValidatorService validatorService;

	@GetMapping("/public")
	public String publicEndpoint() {
		log.info("Public endpoint accessed");
		return "This is a public endpoint. No authentication required.";
	}

	@GetMapping("/secured")
	public String securedEndpoint(Authentication authentication, @RequestHeader(value = "x-trace-id", required = false)
	String traceId) {
		String username = authentication.getName();

		log.info("traceId:{} | Secured endpoint accessed by: {}", traceId, username);
		return "Hello, " + username + "! You have accessed a secured endpoint.";
	}

	@PostMapping("/google")
	public ResponseEntity<GoogleUserDto> authenticate(@RequestBody
	Map<String, String> request, @RequestHeader(value = "x-trace-id", required = false)
	String traceId) {
		String idToken = request.get("idToken");
		GoogleUserDto user = validatorService.validateGoogleSsoToken(traceId, idToken);
		return ResponseEntity.ok(user);
	}
	
	  @PostMapping("/microsoft")
	    public ResponseEntity<?> validateMicrosoftToken(
	            @RequestHeader(value = "x-trace-id", required = false) String traceId,
	            @RequestBody Map<String, String> request) {
	        try {
	            String token = request.get("idToken");
	            MicrosoftUserDto userDto = validatorService.validateMicrosoftSsoToken(traceId, token);
	            return ResponseEntity.ok(userDto);
	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token: " + e.getMessage());
	        }
	    }

	  
}
