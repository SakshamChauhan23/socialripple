package com.social.ripple.external_ingestion.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "external_platforms")
@Getter
@Setter
public class ExternalPlatform {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "platform_name")
    private String platformName;

    @Column(name = "api_url")
    private String apiUrl;

    @Column(name = "credentials", columnDefinition = "TEXT")
    private String credentials;

    @Column(name = "enabled")
    private Boolean enabled = Boolean.TRUE;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_sync_attempt_at")
    private LocalDateTime lastSyncAttemptAt;

    @Column(name = "last_sync_success_at")
    private LocalDateTime lastSyncSuccessAt;

    @Column(name = "last_sync_status")
    private String lastSyncStatus;

    @Column(name = "last_sync_error", columnDefinition = "TEXT")
    private String lastSyncError;

    @Column(name = "last_sync_error_at")
    private LocalDateTime lastSyncErrorAt;

    @Column(name = "last_imported_count")
    private Integer lastImportedCount;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
