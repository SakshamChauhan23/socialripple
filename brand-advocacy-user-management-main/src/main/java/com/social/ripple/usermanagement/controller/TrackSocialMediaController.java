/**
 * Filename: TrackSocialMediaController.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all
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
package com.social.ripple.usermanagement.controller;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.ITrackSocialMediaService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping
@Slf4j
public class TrackSocialMediaController {

	private final ITrackSocialMediaService trackSocialMediaService;
	public TrackSocialMediaController(ITrackSocialMediaService trackSocialMediaService) {
		this.trackSocialMediaService = trackSocialMediaService;
	}

	@GetMapping("{version}/socialmedia/track/{userId}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> fetchSocialMediaConnection(@PathVariable("version")
	String version,@PathVariable("userId") Long userId,@AuthenticationPrincipal
	UserDetailsImpl userDetails, @RequestHeader(value = "x-trace-id", required = true)
	String traceId, @RequestHeader(value = "x-tenant-id", required = true)
	String tenantId){
		try {
			BaseResponse response = trackSocialMediaService.trackSocialMediaConnection(traceId, tenantId, userDetails, userId,false);
			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}

			return ResponseEntity.ok(response);

		}
		catch (Exception ex) {
			BaseResponse errorResponse = new BaseResponse();
			errorResponse.setStatus(false);
			errorResponse.setCode("USMG_500");
			errorResponse.setMessage("Unhandled error during fetching sociall media connection for a user");
			errorResponse.setTimestamp(new Date());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

	@GetMapping("{version}/socialmedia/track-self")
	public ResponseEntity<BaseResponse> fetchSocialMediaConnection(@PathVariable("version")
																   String version, @AuthenticationPrincipal
																   UserDetailsImpl userDetails, @RequestHeader(value = "x-trace-id", required = true)
																   String traceId, @RequestHeader(value = "x-tenant-id", required = true)
																   String tenantId){
		try {
			BaseResponse response = trackSocialMediaService.trackSocialMediaConnection(traceId, tenantId, userDetails, null,true);
			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}

			return ResponseEntity.ok(response);

		}
		catch (Exception ex) {
			BaseResponse errorResponse = new BaseResponse();
			errorResponse.setStatus(false);
			errorResponse.setCode("USMG_500");
			errorResponse.setMessage("Unhandled error during fetching sociall media connection for a user");
			errorResponse.setTimestamp(new Date());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

	@GetMapping("{version}/socialmedia/business-pages")
	public ResponseEntity<BaseResponse> fetchBusinessPageLinks(@PathVariable("version")
			String version, @AuthenticationPrincipal UserDetailsImpl userDetails,
			@RequestHeader(value = "x-trace-id", required = true) String traceId,
			@RequestHeader(value = "x-tenant-id", required = true) String tenantId) {
		try {
			BaseResponse response = trackSocialMediaService.getBusinessPageLinks(traceId, tenantId, userDetails);
			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}

			return ResponseEntity.ok(response);

		} catch (Exception ex) {
			BaseResponse errorResponse = new BaseResponse();
			errorResponse.setStatus(false);
			errorResponse.setCode("USMG_500");
			errorResponse.setMessage("Unhandled error during fetching business page links");
			errorResponse.setTimestamp(new Date());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
		}
	}

	@PostMapping("{version}/socialmedia/remainder/{userId}/{platform}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public ResponseEntity<BaseResponse> sendRemainderToUser(
	        @PathVariable("version") String version,
	        @PathVariable("userId") Long userId, @PathVariable("platform") String platform,
	        @AuthenticationPrincipal UserDetailsImpl userDetails,
	        @RequestHeader(value = "x-trace-id", required = true) String traceId,
	        @RequestHeader(value = "x-tenant-id", required = true) String tenantId) {

	    try {
	        BaseResponse response = trackSocialMediaService.sendSocialMediaRemainder(traceId, tenantId, userDetails, userId, platform);

	        if (ResponseCode.USMG_401.equals(response.getCode())) {
	            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
	        }

	        return ResponseEntity.status(ResponseCode.USMG_200.equals(response.getCode()) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
	                             .body(response);

	    } catch (Exception ex) {
	        BaseResponse errorResponse = new BaseResponse();
	        errorResponse.setStatus(false);
	        errorResponse.setCode(ResponseCode.USMG_500);
	        errorResponse.setMessage("Unhandled error during sending social media remainder for a user");
	        errorResponse.setTimestamp(new Date());
	        log.error("[{}]|SOCIAL|REMAINDER_EXCEPTION|UserId:{}|{}", traceId, userId, ex.getMessage(), ex);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	    }
	}
}
