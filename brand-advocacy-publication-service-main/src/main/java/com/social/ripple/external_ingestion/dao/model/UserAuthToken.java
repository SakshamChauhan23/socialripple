/**
 * Filename: UserAuthToken.java
 *
 * © Copyright 2024 Quasarix. ALL RIGHTS RESERVED.

 * All rights, title and interest (including all intellectual property rights) in this software and any derivative works based upon or derived from
 * this software belongs exclusively to Quasarix.

 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment,
 * the license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies.

 * This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix.

 * Any use, reproduction, modification, distribution, public performance or display of this software or through the use of this software without the
 * prior, express written consent of Quasarix is strictly prohibited and may be in violation of applicable laws.
 *
 */
package com.social.ripple.external_ingestion.dao.model;

import java.time.LocalDateTime;

import com.social.ripple.external_ingestion.util.enumeration.AuthStatus;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import com.social.ripple.external_ingestion.util.enumeration.TransactionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_auth_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAuthToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id")
	private Long userId; // Local user id

	@Enumerated(EnumType.STRING)
	@Column(name = "platform", nullable = false)
	private Platform platform; // e.g., INSTAGRAM, FACEBOOK, LINKEDIN, X

	@Column(name = "user_id_external", length = 50)
	private String userIdExternal; // External user id (Twitter/X)

	@Column(name = "access_token", length = 512)
	private String accessToken;

	@Column(name = "access_secret", length = 512)
	private String accessSecret;

	@Column(name = "refresh_token", length = 512)
	private String refreshToken;

	@Column(name = "expire_at")
	private LocalDateTime expireAt;

	@Column(name = "client_id", length = 255)
	private String clientId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private AuthStatus status = AuthStatus.NEVER_CONNECTED; // NEVER_CONNECTED, PENDING_CONNECTION, CONNECTED,
															// DISCONNECTED

	@Column(name = "is_connected")
	private Boolean isConnected = false;

	@Column(name = "transaction_id", length = 255, unique = true)
	private String transactionId;

	@Column(name = "transaction_created_at")
	private LocalDateTime transactionCreatedAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "transaction_status")
	private TransactionStatus transactionStatus;

	@Column(name = "oauth_source_page", length = 32)
	private String oauthSourcePage;

	@Column(name = "tenant_id")
	private Long tenantId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@Column(name = "last_sync_attempt_at")
	private LocalDateTime lastSyncAttemptAt;

	@Column(name = "last_sync_success_at")
	private LocalDateTime lastSyncSuccessAt;

	@Column(name = "last_sync_status", length = 50)
	private String lastSyncStatus;

	@Column(name = "last_sync_error", columnDefinition = "TEXT")
	private String lastSyncError;

	@Column(name = "last_sync_error_at")
	private LocalDateTime lastSyncErrorAt;

}
