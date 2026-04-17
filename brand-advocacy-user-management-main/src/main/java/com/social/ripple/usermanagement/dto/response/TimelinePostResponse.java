package com.social.ripple.usermanagement.dto.response;

import java.util.Date;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TimelinePostResponse extends BaseResponse {

	private List<UserTimelineDto> posts;
	private long totalElements;
	private int totalPages;
	private int page;
	private Date timestamp;
	private UserTimelineDto preparedPost;
	private UserTimelineDto post;
	private Map<String,String> platformContentMap;
	private String xHashtags;
	private List<ShareUserDTO> shareUserDTOs;
	private LoyaltyPointDTO estimatedLoyaltyPoints;
}
