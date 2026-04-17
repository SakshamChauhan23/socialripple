package com.social.ripple.usermanagement.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignCategoryResponse extends BaseResponse {

	private Long postId;
	private List<Long> categoryIds;  

}
