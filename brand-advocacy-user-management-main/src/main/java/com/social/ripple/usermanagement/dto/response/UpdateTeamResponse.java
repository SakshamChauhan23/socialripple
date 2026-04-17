package com.social.ripple.usermanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for updating a team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTeamResponse extends BaseResponse {

    private Long teamId;
    private String newTeamName;
    private String imageUrl;

}
