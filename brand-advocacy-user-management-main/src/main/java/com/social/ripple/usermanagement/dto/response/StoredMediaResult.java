package com.social.ripple.usermanagement.dto.response;

import com.social.ripple.usermanagement.util.enumeration.MediaStorageProvider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredMediaResult {
	private String fileId;
	private String fileName;
	private String fileUrl;
	private String providerAssetId;
	private String playbackUrl;
	private String thumbnailUrl;
	private String processingStatus;
	private MediaStorageProvider storageProvider;
}
