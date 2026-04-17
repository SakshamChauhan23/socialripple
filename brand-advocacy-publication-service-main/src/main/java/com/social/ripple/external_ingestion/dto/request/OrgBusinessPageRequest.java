package com.social.ripple.external_ingestion.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrgBusinessPageRequest {
    private Boolean enabled;
    private String apiUrl;
    private String accessToken;
    private String refreshToken;
    private String externalUserId;
    private String pageId;
    private String username;
    private String pageUrl;
    private String businessPageLink;
    private String displayName;
}
