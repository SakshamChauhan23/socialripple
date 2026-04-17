package com.social.ripple.usermanagement.service;

import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import org.springframework.data.domain.Pageable;

public interface ITimelineService {
	BaseResponse getTimelinePosts(String traceId, Long postId, Long tenantId, Long categoryId, String platform,
			boolean leader, String searchText, Long userId, Pageable pageable, UserDetailsImpl detailsImpl);

	BaseResponse getPostForPlatformShare(String traceId, Long postId, Long tenantId, UserDetailsImpl detailsImpl, Boolean isForShare);

	BaseResponse getActivitiesPosts(String traceId, Long postId, Long tenantId, Long userId, Pageable pageable, UserDetailsImpl detailsImpl);
	BaseResponse getActivitiesShares(String traceId, Long postId, Long tenantId, Long userId, Pageable pageable, UserDetailsImpl detailsImpl);
	BaseResponse getActivitiesTags(String traceId, Long postId, Long tenantId, Long userId, Pageable pageable, UserDetailsImpl detailsImpl);

	BaseResponse getPostDetailById(String traceId, Long postId, Long tenantId, Pageable pageable, UserDetailsImpl detailsImpl);

	BaseResponse getActivitiesPostSharedUsers(String traceId, Long postId, Long tenantId, Pageable pageable, UserDetailsImpl detailsImpl);
}
