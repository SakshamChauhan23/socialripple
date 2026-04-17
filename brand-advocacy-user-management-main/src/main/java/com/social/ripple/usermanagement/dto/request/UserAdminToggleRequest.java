package com.social.ripple.usermanagement.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserAdminToggleRequest {
	private Long userId;
	private Boolean enableAdmin;
}
