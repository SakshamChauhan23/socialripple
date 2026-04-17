/**
 * Filename: ActivityController.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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

import com.social.ripple.usermanagement.service.ITimelineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.social.ripple.usermanagement.dto.response.ActivityShareResponse;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.PostActivityResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IActivityService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

@Slf4j
@RestController
@RequestMapping("{version}/user")
public class ActivityController {
	private final IActivityService activityService;

	@Autowired
	private ITimelineService timelineService;

	public ActivityController(IActivityService activityService) {
		this.activityService = activityService;
	}


	@GetMapping("activities/platforms/share/count")
	public ResponseEntity<BaseResponse> fetchPlatformShareCount(@PathVariable("version")
																  String version,@AuthenticationPrincipal
																  UserDetailsImpl userDetails, @RequestHeader(value = "x-trace-id", required = true)
																  String traceId, @RequestHeader(value = "x-tenant-id", required = true)
																  String tenantId,@RequestParam(required = false) Long userId) {
		try {
			ActivityShareResponse response = activityService.fetchPlatformShareCount(traceId, tenantId, userDetails, userId);
			response.setTimestamp(new Date());

			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			return buildErrorResponse("activities/posts", ResponseCode.USMG_500, "Internal server error", e.getMessage(),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("activities/posts")
	public ResponseEntity<BaseResponse> getActivitiesPosts(@PathVariable("version") String version,
														 @RequestHeader(value = "x-trace-id", required = true) String traceId,
														 @RequestHeader(value = "x-tenant-id", required = true) Long tenantId,
														 @AuthenticationPrincipal UserDetailsImpl userDetails,
														 @RequestParam(value = "postId", required = false) Long postId,
														   @RequestParam(value = "userId", required = false) Long userId,
														 @RequestParam(value = "page", defaultValue = "0") int page,
														 @RequestParam(value = "size", defaultValue = "10") int size) {

		log.info("[{}]|TIMELINE|GET|Fetching timeline posts for tenantId: {}", traceId, tenantId);

		try {

			Pageable pageable = PageRequest.of(page, size);

			BaseResponse response = timelineService.getActivitiesPosts(traceId, postId, tenantId, userId, pageable, userDetails);

			response.setTimestamp(new Date());

			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
			if (ResponseCode.USMG_404.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, e.getMessage(), e);

			BaseResponse response = new BaseResponse();
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal Server Error");
			response.setDevMessage(e.getMessage());
			response.setTimestamp(new Date());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("activities/shares")
	public ResponseEntity<BaseResponse> getActivitiesShares(@PathVariable("version") String version,
														   @RequestHeader(value = "x-trace-id", required = true) String traceId,
														   @RequestHeader(value = "x-tenant-id", required = true) Long tenantId,
														   @AuthenticationPrincipal UserDetailsImpl userDetails,
														   @RequestParam(value = "postId", required = false) Long postId,
														   @RequestParam(value = "userId", required = false) Long userId,
														   @RequestParam(value = "page", defaultValue = "0") int page,
														   @RequestParam(value = "size", defaultValue = "10") int size) {

		log.info("[{}]|TIMELINE|GET|Fetching timeline posts for tenantId: {}", traceId, tenantId);

		try {

			Pageable pageable = PageRequest.of(page, size);

			BaseResponse response = timelineService.getActivitiesShares(traceId, postId, tenantId, userId, pageable, userDetails);

			response.setTimestamp(new Date());

			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
			if (ResponseCode.USMG_404.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, e.getMessage(), e);

			BaseResponse response = new BaseResponse();
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal Server Error");
			response.setDevMessage(e.getMessage());
			response.setTimestamp(new Date());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@GetMapping("activities/tags")
	public ResponseEntity<BaseResponse> getActivitiesTags(@PathVariable("version") String version,
														   @RequestHeader(value = "x-trace-id", required = true) String traceId,
														   @RequestHeader(value = "x-tenant-id", required = true) Long tenantId,
														   @AuthenticationPrincipal UserDetailsImpl userDetails,
														   @RequestParam(value = "postId", required = false) Long postId,
														   @RequestParam(value = "userId", required = false) Long userId,
														   @RequestParam(value = "page", defaultValue = "0") int page,
														   @RequestParam(value = "size", defaultValue = "10") int size) {

		log.info("[{}]|TIMELINE|GET|Fetching timeline posts for tenantId: {}", traceId, tenantId);

		try {

			Pageable pageable = PageRequest.of(page, size);

			BaseResponse response = timelineService.getActivitiesTags(traceId, postId, tenantId, userId, pageable, userDetails);

			response.setTimestamp(new Date());

			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
			if (ResponseCode.USMG_404.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, e.getMessage(), e);

			BaseResponse response = new BaseResponse();
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal Server Error");
			response.setDevMessage(e.getMessage());
			response.setTimestamp(new Date());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}


	@GetMapping("activities/post/shared-users")
	public ResponseEntity<BaseResponse> getActivitiesPostSharedUsers(@PathVariable("version") String version,
														  @RequestHeader(value = "x-trace-id", required = true) String traceId,
														  @RequestHeader(value = "x-tenant-id", required = true) Long tenantId,
														  @AuthenticationPrincipal UserDetailsImpl userDetails,
														  @RequestParam(value = "postId", required = false) Long postId,
														  @RequestParam(value = "page", defaultValue = "0") int page,
														  @RequestParam(value = "size", defaultValue = "10") int size) {

		log.info("[{}]|TIMELINE|GET|Fetching timeline posts for tenantId: {}", traceId, tenantId);

		try {

			Pageable pageable = PageRequest.of(page, size);

			BaseResponse response = timelineService.getActivitiesPostSharedUsers(traceId, postId, tenantId, pageable, userDetails);

			response.setTimestamp(new Date());

			if (ResponseCode.USMG_401.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}
			if (ResponseCode.USMG_404.equals(response.getCode())) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}

			return ResponseEntity.ok(response);

		} catch (Exception e) {
			log.error("[{}]|TIMELINE|GET_FAILED|Exception: {}", traceId, e.getMessage(), e);

			BaseResponse response = new BaseResponse();
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal Server Error");
			response.setDevMessage(e.getMessage());
			response.setTimestamp(new Date());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}


	private ResponseEntity<BaseResponse> buildErrorResponse(String path, String code, String error, String message, HttpStatus status) {
		ErrorObj errorObj = new ErrorObj(path, code, error, message);
		BaseResponse baseResponse = new BaseResponse();
		baseResponse.setStatus(false);
		baseResponse.setCode(code);
		baseResponse.setMessage(error);
		baseResponse.setDevMessage(message);
		baseResponse.setTimestamp(new Date());
		baseResponse.setErrors(List.of(errorObj));
		return new ResponseEntity<>(baseResponse, status);
	}
}
