package com.social.ripple.usermanagement.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostCreatorDto {
	private Long id;
	private String name;
	private String teamName;
	private String profileImageUrl;
}