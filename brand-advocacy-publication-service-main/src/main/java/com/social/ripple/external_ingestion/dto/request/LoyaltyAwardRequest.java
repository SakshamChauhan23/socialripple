package com.social.ripple.external_ingestion.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoyaltyAwardRequest {

	private Long userId;
	private Long creditId;
	private Boolean isShare;


}
