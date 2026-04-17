/**
 * Filename: PostRepository.java
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

import com.social.ripple.external_ingestion.dao.model.Post;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("SELECT t.platformUniqueId FROM Post t WHERE t.type = :platform AND t.organization.id = :organizationId")
    Set<String> findPlatformUniqueIdsByPlatformAndOrganization(@Param("platform") String platform,@Param("organizationId") Long organizationId);

    @Query("SELECT t.platformUniqueId FROM Post t WHERE t.type = :platform AND t.organization.id = :organizationId AND t.createdBy.id = :createdById")
    Set<String> findPlatformUniqueIdsByPlatformAndOrganizationAndCreatedBy(@Param("platform") String platform,
                                                                           @Param("organizationId") Long organizationId,
                                                                           @Param("createdById") Long createdById);

    @Query("SELECT p.platformUniqueId FROM Post p WHERE p.syncedAt IS NULL OR p.syncedAt < :oneDayAgo and type=:type")
    List<String> findPostIdsForSync(@Param("oneDayAgo") LocalDateTime oneDayAgo, String type);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.syncedAt = :currentTime WHERE p.platformUniqueId IN :platformIds")
    int updateSyncedAtForPlatformIds(
            @Param("platformIds") List<String> platformIds,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Modifying
    @Transactional
    @Query("DELETE FROM Post p WHERE p.platformUniqueId IN :platformIds")
    int deleteByPlatformUniqueIdIn(@Param("platformIds") List<String> platformIds);
}
