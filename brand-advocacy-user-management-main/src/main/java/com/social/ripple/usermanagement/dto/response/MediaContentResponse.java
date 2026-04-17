package com.social.ripple.usermanagement.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import org.springframework.core.io.Resource;

@Data
public class MediaContentResponse {
	private boolean status;
	private String code;
	private String message;
	private String timestamp;

	private Long id;
	private Long organizationId;
	private String title;
	private String fileType;
	private String url;
	private Long createdBy;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private Long updatedBy;
	private Boolean archived;
	private Resource resource;
}
