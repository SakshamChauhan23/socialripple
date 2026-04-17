package com.social.ripple.usermanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTeamResponse extends BaseResponse {

	private Long teamId;
	private String teamName;
	private String imageCode;
}
