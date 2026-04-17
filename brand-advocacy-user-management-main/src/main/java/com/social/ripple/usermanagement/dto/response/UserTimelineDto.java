package com.social.ripple.usermanagement.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserTimelineDto {

	private Long id;
	private Long organizationId;
	private String title;
	private String content;
	private String type;
	private String sourceType;
	private String status;
	private LocalDateTime scheduledAt;
	private LocalDateTime createdAt;
	private LocalDateTime platformCreatedAt;
	private LocalDateTime updatedAt;
	private PostCreatorDto createdBy;
	private List<MediaPostDto> media;
	private List<PostHashtagsDto> hashtags;
	private List<PostTagDto> taggedUsers;
	private List<PostCategoryMapDto> categories;
	private PostShareDataDTO postShareData;
	private Integer earnedLoyaltyPoint;
	private String sourceName;
	private String sourceUsername;
	private String sourceAvatarUrl;
}
