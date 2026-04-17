package com.social.ripple.external_ingestion.dao.model;

import com.social.ripple.external_ingestion.util.enumeration.Platform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "org_business_connect_tx")
@Getter
@Setter
@NoArgsConstructor
public class OrgBusinessConnectTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 255)
    private String transactionId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "initiated_by_user_id", nullable = false)
    private Long initiatedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 50)
    private Platform platform;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "temporary_access_token", columnDefinition = "TEXT")
    private String temporaryAccessToken;

    @Column(name = "temporary_refresh_token", columnDefinition = "TEXT")
    private String temporaryRefreshToken;

    @Column(name = "temporary_access_secret", columnDefinition = "TEXT")
    private String temporaryAccessSecret;

    @Column(name = "provider_user_id", length = 255)
    private String providerUserId;

    @Column(name = "provider_username", length = 255)
    private String providerUsername;

    @Column(name = "provider_display_name", length = 512)
    private String providerDisplayName;

    @Column(name = "discovered_pages_json", columnDefinition = "LONGTEXT")
    private String discoveredPagesJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
