package com.social.ripple.usermanagement.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO for deleting a team
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeleteTeamResponse extends BaseResponse {

    private Long deletedTeamId;
    private String deletedTeamName;

}
