package com.social.ripple.usermanagement.dto.response;

import java.util.List;

import com.social.ripple.usermanagement.dto.UserDto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GetAllUsersResponse extends BaseResponse {
	private List<UserDto> users;
	private long totalElements;
	private int totalPages;
	private int page;
}
