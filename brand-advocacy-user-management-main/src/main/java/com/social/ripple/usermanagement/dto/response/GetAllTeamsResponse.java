package com.social.ripple.usermanagement.dto.response;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GetAllTeamsResponse extends BaseResponse {
	private List<TeamSummary> teams;
}
