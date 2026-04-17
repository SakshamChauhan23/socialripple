package com.social.ripple.usermanagement.dto.response;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GetAllRolesResponse extends BaseResponse {
    private List<RoleResponse> roles;
}
