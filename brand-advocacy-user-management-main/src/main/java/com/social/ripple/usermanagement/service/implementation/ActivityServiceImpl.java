/**
 * Filename: ActivityServiceImpl.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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

import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.social.ripple.usermanagement.dao.model.ExternalShare;
import com.social.ripple.usermanagement.dao.model.Post;
import com.social.ripple.usermanagement.dao.repository.ExternalShareRepository;
import com.social.ripple.usermanagement.dao.repository.PostMediaRepository;
import com.social.ripple.usermanagement.dao.repository.PostRepository;
import com.social.ripple.usermanagement.dto.response.ActivityShareDto;
import com.social.ripple.usermanagement.dto.response.ActivityShareResponse;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.PostActivityDto;
import com.social.ripple.usermanagement.dto.response.PostActivityResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.IActivityService;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class ActivityServiceImpl implements IActivityService {

	private final PostRepository postRepository;
	private final ExternalShareRepository externalShareRepository;
	private final PostMediaRepository postMediaRepository;
	public ActivityServiceImpl(PostRepository postRepository, ExternalShareRepository externalShareRepository,
			PostMediaRepository postMediaRepository) {
		this.postRepository = postRepository;
		this.externalShareRepository = externalShareRepository;
		this.postMediaRepository = postMediaRepository;
	}

	@Override
	public PostActivityResponse fetchActivityPostDetails(String traceId, String tenantId, UserDetailsImpl userDetails, int page, int size) {
		Date timestamp = new Date();
		log.info("[{}]|POST|ACTIVITY|FETCH_INIT|Fetching post activities for user", traceId);

		PostActivityResponse response = new PostActivityResponse();

		try {
			if (userDetails == null) {
				log.warn("[{}]|POST|ACTIVITY|UNAUTHORIZED|Missing user details", traceId);
				response.setStatus(false);
				response.setCode(ResponseCode.USMG_401);
				response.setMessage("Unauthorized");
				response.setDevMessage("UserDetails_or_userId_is_null");
				response.setTimestamp(timestamp);
				response.setErrors(List.of(new ErrorObj("activities/posts", ResponseCode.USMG_401, "Unauthorized", "UserDetails or userId is null")));
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
				response.setActivitiesPost(Collections.emptyList());
				response.setErrors(Collections.emptyList());
				return response;
			}
			Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
			Page<Post> postPage = postRepository.findByCreatedByOrderByPlatformCreatedAtDescCreatedAtDesc(userDetails.getUserId(), pageable);

			List<PostActivityDto> activityDtoList = new ArrayList<>();
			for (Post post : postPage.getContent()) {
				if (post != null && post.getId() != null) {
					PostActivityDto dto = new PostActivityDto();
					dto.setPostId(post.getId());
					dto.setContent(post.getContent());
					dto.setCreatedAt(post.getCreatedAt());
					List<String> mediaUrl = postMediaRepository.findUrlsByPostId(post.getId());
					dto.setMediaUrl(mediaUrl);
					activityDtoList.add(dto);
				}
			}

			log.info("[{}]|POST|ACTIVITY|SUCCESS|Fetched {} posts", traceId, activityDtoList.size());
			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("Post activities fetched successfully");
			response.setDevMessage("User's post data retrieved");
			response.setTimestamp(timestamp);
			response.setActivitiesPost(activityDtoList);
			response.setTotalElements(postPage.getTotalElements());
			response.setTotalPages(postPage.getTotalPages());
			return response;

		}
		catch (Exception ex) {
			log.error("[{}]|POST|ACTIVITY|EXCEPTION|{}", traceId, ex.getMessage(), ex);
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal_server_error");
			response.setDevMessage(ex.getMessage());
			response.setTimestamp(timestamp);
			response.setErrors(List.of(new ErrorObj("activities/posts", ResponseCode.USMG_500, "Internal server error", ex.getMessage())));
			return response;
		}
	}

	@Override
	public ActivityShareResponse fetchActivityShareDetails(String traceId, String tenantId, UserDetailsImpl userDetails, int page, int size) {
		Date timestamp = new Date();
		log.info("[{}]|POST|SHARE|FETCH_INIT|Fetching shared activities", traceId);

		ActivityShareResponse response = new ActivityShareResponse();

		try {
			if (userDetails == null) {
				log.warn("[{}]|POST|SHARE|UNAUTHORIZED|Missing user details", traceId);
				response.setStatus(false);
				response.setCode(ResponseCode.USMG_401);
				response.setMessage("Unauthorized");
				response.setDevMessage("UserDetails or userId is null");
				response.setTimestamp(timestamp);
				response.setErrors(List.of(new ErrorObj("activities/share", ResponseCode.USMG_401, "Unauthorized", "UserDetails or userId is null")));
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
				response.setSharedActivities(Collections.emptyList());
				response.setErrors(Collections.emptyList());
				return response;
			}

			Pageable pageable = PageRequest.of(page, size, Sort.by("sharedAt").descending());
			Page<ExternalShare> sharePage = externalShareRepository.findByUserIdOrderBySharedAtDesc(userDetails.getUserId(), pageable);

			List<ActivityShareDto> shareDtoList = new ArrayList<>();
			for (ExternalShare share : sharePage.getContent()) {
				if (share != null && share.getId() != null) {
					ActivityShareDto dto = new ActivityShareDto();
					dto.setShareId(share.getId());
					dto.setPostId(share.getPostId());
					dto.setCaption(share.getCaption());
					dto.setSharedAt(share.getSharedAt());
					dto.setPlatforms(List.of(share.getPlatform()));
					List<String> mediaUrl = postMediaRepository.findUrlsByPostId(share.getPostId());
					dto.setMediaUrl(mediaUrl);
					shareDtoList.add(dto);
				}
			}

			log.info("[{}]|POST|SHARE|SUCCESS|Fetched {} share records", traceId, shareDtoList.size());
			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("Shared activities fetched successfully");
			response.setDevMessage("User's shared activity data retrieved");
			response.setTimestamp(timestamp);
			response.setSharedActivities(shareDtoList);
			return response;

		}
		catch (Exception ex) {
			log.error("[{}]|POST|SHARE|EXCEPTION|{}", traceId, ex.getMessage(), ex);
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal server error");
			response.setDevMessage(ex.getMessage());
			response.setTimestamp(timestamp);
			response.setErrors(List.of(new ErrorObj("activities/share", ResponseCode.USMG_500, "Internal server error", ex.getMessage())));
			return response;
		}
	}


	@Override
	public ActivityShareResponse fetchPlatformShareCount(String traceId, String tenantId, UserDetailsImpl userDetails, Long userId) {
		Date timestamp = new Date();
		log.info("[{}]|POST|SHARE|FETCH_INIT|Fetching shared activities", traceId);

		ActivityShareResponse response = new ActivityShareResponse();

		try {
			if (userDetails == null) {
				log.warn("[{}]|POST|SHARE|UNAUTHORIZED|Missing user details", traceId);
				response.setStatus(false);
				response.setCode(ResponseCode.USMG_401);
				response.setMessage("Unauthorized");
				response.setDevMessage("UserDetails or userId is null");
				response.setTimestamp(timestamp);
				response.setErrors(List.of(new ErrorObj("activities/share", ResponseCode.USMG_401, "Unauthorized", "UserDetails or userId is null")));
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
				response.setSharedActivities(Collections.emptyList());
				response.setErrors(Collections.emptyList());
				return response;
			}

			List<Object[]> results = externalShareRepository.getPlatformWiseCountNative(userId != null? userId: userDetails.getUserId());

			Map<String, Long> platformCountMap = new HashMap<>();
			for (Object[] result : results) {
				String platform = (String) result[0];
				Long count = (Long) result[1];
				platformCountMap.put(platform, count);
			}

			response.setPlatformCountMap(platformCountMap);

			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("Shared activities fetched successfully");
			response.setDevMessage("User's shared activity data retrieved");
			response.setTimestamp(timestamp);
			return response;

		}
		catch (Exception ex) {
			log.error("[{}]|POST|SHARE|EXCEPTION|{}", traceId, ex.getMessage(), ex);
			response.setStatus(false);
			response.setCode(ResponseCode.USMG_500);
			response.setMessage("Internal server error");
			response.setDevMessage(ex.getMessage());
			response.setTimestamp(timestamp);
			response.setErrors(List.of(new ErrorObj("activities/share", ResponseCode.USMG_500, "Internal server error", ex.getMessage())));
			return response;
		}
	}
}