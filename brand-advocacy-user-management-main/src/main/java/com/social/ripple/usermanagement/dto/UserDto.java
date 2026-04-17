package com.social.ripple.usermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor

public class UserDto {
	private Long id;
	private String name;
	private String email;
	private String phone;
	private String profilePictureUrl;
	private String status;
	private Boolean isLeader;
	private String department;
	private String jobTitle;
	private Boolean isAdmin;
}
