package com.social.ripple.usermanagement.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteUserResponse extends BaseResponse {
	private String userId;
	private String email;
	private String invitationToken;
}
