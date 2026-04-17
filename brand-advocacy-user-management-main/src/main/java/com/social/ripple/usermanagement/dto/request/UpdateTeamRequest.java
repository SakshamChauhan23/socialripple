package com.social.ripple.usermanagement.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTeamRequest {
	private String newTeamName;
	private String imageUrl;
}
