package com.social.ripple.usermanagement.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response for fetching a team by ID along with its members.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GetTeamDetailsResponse extends BaseResponse {

	private Long teamId;
	private String teamName;
	private String createdBy;
	private LocalDateTime createdAt;
	private List<TeamMemberDto> members;

}

