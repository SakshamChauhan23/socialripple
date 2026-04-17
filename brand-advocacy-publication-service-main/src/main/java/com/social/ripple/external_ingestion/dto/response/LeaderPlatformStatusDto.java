package com.social.ripple.external_ingestion.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class LeaderPlatformStatusDto {
    private String platform;
    private String status;
    private String externalUserId;
    private String updatedAt;
    private String lastSyncAttemptAt;
    private String lastSyncSuccessAt;
    private String lastSyncStatus;
    private String lastSyncError;
    private String lastSyncErrorAt;
}
