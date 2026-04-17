package com.social.ripple.usermanagement.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserStatusResponse extends BaseResponse {

	private Long userId;
	private String newStatus;
}