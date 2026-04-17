package com.social.ripple.external_ingestion.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BusinessPageOptionDto {
    private String selectionId;
    private String platform;
    private String externalUserId;
    private String pageId;
    private String username;
    private String pageUrl;
    private String businessPageLink;
    private String displayName;
}
