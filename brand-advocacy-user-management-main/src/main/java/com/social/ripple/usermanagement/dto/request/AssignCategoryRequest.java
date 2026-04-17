package com.social.ripple.usermanagement.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignCategoryRequest {
	private Long postId;
	private List<Long> categoryIds;
}
