package com.social.ripple.usermanagement.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ToggleConnectionRequest {
	private String platform;
	private boolean connect;
	private String sourcePage;
}
