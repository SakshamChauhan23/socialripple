/**
 * Filename: UserAuthTokenRepository.java
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
package com.social.ripple.external_ingestion.dao.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.social.ripple.external_ingestion.dao.model.UserAuthToken;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import com.social.ripple.external_ingestion.util.enumeration.TransactionStatus;

@Repository
public interface UserAuthTokenRepository extends JpaRepository<UserAuthToken, Long> {

	Optional<UserAuthToken> findByUserIdExternalAndPlatform(String externalUserId, Platform x);

	Optional<UserAuthToken> findByUserIdExternal(String userIdExternal);

	Optional<UserAuthToken> findByTransactionId(String transactionId);

	Optional<UserAuthToken> findByTransactionIdAndPlatform(String transactionId,Platform x);

	Optional<UserAuthToken> findByUserIdAndPlatform(Long userId, Platform x);

	Optional<UserAuthToken> findByAccessTokenAndPlatformAndTransactionStatus(String accessToken, Platform platform, TransactionStatus transactionStatus);

	Optional<UserAuthToken> findFirstByUserIdExternalAndPlatformAndTenantId(String userIdExternal, Platform platform, Long tenantId);

	List<UserAuthToken> findByUserIdIn(Collection<Long> userIds);

	@Query("""
			SELECT t FROM UserAuthToken t
			JOIN User u ON u.id = t.userId
			WHERE t.platform = :platform
			  AND (t.isConnected = true OR t.status = com.social.ripple.external_ingestion.util.enumeration.AuthStatus.CONNECTED)
			  AND t.accessToken IS NOT NULL
			  AND u.isLeader = true
			""")
	List<UserAuthToken> findConnectedLeaderTokensByPlatform(@Param("platform") Platform platform);

}
