package com.social.ripple.usermanagement.dto.request;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class InviteUserRequest {
	private Long id;
	private String name;
	private String email;
	private String mobileNumber;
	private List<String> role;
	private String department;
	private String jobTitle;
	private Long profilePictureId;
	private String profilePicture;
}
