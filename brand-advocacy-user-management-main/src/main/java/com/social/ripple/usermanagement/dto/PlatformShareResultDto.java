package com.social.ripple.usermanagement.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlatformShareResultDto {
	private String platform;
	private boolean success;
	private String message;
	private String redirectUrl; }
