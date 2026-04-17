package com.social.ripple.external_ingestion.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OrgBusinessPageDto {
    private String platform;
    private boolean connected;
    private boolean enabled;
    private boolean oauthSupported;
    private boolean manualFallbackAvailable;
    private String oauthProviderLabel;
    private String supportMessage;
    private String apiUrl;
    private String configSource;
    private String accessTokenPreview;
    private boolean accessTokenConfigured;
    private String refreshTokenPreview;
    private boolean refreshTokenConfigured;
    private String externalUserId;
    private String pageId;
    private String username;
    private String pageUrl;
    private String businessPageLink;
    private String displayName;
    private String lastSyncAttemptAt;
    private String lastSyncSuccessAt;
    private String lastSyncStatus;
    private String lastSyncError;
    private String lastSyncErrorAt;
    private Integer lastImportedCount;
    private String scheduleCadenceLabel;
    private String nextScheduledFetchAt;
}
