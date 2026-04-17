package com.social.ripple.usermanagement.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ToggleConnectionResponse extends BaseResponse {
	private boolean isConnected;
	private String platform;
	private String message;
	private String authUrl;
}
