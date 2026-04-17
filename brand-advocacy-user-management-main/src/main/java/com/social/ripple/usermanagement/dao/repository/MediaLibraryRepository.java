/**
 * Filename: MediaLibraryRepository.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
 * property rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this
 * software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements
 * explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use this software
 * internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures, routines,
 * customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted above, no
 * license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the license
 * granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any copies. This
 * software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public performance or
 * display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly prohibited and may
 * be in violation of applicable laws.
 */
package com.social.ripple.usermanagement.dao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.social.ripple.usermanagement.dao.model.MediaLibrary;

public interface MediaLibraryRepository extends JpaRepository<MediaLibrary, Long> {
	Page<MediaLibrary> findByOrganizationIdAndFileType(Long orgId, String fileType, Pageable pageable);

	@Query("""
			SELECT ml
			FROM MediaLibrary ml
			JOIN MediaCategoryMap mcm ON ml.id = mcm.libraryId
			WHERE ml.organizationId = :orgId
			  AND ml.fileType = :fileType
			  AND mcm.categoryId = :categoryId
			""")
	Page<MediaLibrary> findByOrgAndTypeAndCategory(@Param("orgId")
	Long orgId, @Param("fileType")
	String fileType, @Param("categoryId")
	Long categoryId, Pageable pageable);

	boolean existsByOrganizationIdAndUrl(Long orgId, String fileUrl);

	Optional<MediaLibrary> findByIdAndOrganizationId(Long id, Long organizationId);

    Page<MediaLibrary> findByOrganizationIdAndFileTypeAndArchived(Long organizationId,
                                                                  String fileType,
                                                                  Boolean archived,
                                                                  Pageable pageable);

    @Query(value = """
            SELECT ml
            FROM MediaLibrary ml
            JOIN MediaCategoryMap mcm ON mcm.libraryId = ml.id
            WHERE ml.organizationId = :orgId
              AND ml.fileType = :fileType
              AND ml.archived = :archived
              AND mcm.categoryId = :categoryId
            """)
    Page<MediaLibrary> findByOrgAndTypeAndCategoryAndArchived(@Param("orgId") Long orgId,
                                                              @Param("fileType") String fileType,
                                                              @Param("categoryId") Long categoryId,
                                                              @Param("archived") Boolean archived,
                                                              Pageable pageable);


    // For employees: only NON-archived records
    @Query("SELECT m FROM MediaLibrary m WHERE m.organizationId = :orgId AND m.archived = false")
    List<MediaLibrary> findActiveMediaByOrg(@Param("orgId") Long orgId);

    // For admins: both archived and non-archived
    List<MediaLibrary> findByOrganizationIdAndArchived(Long organizationId, Boolean archived);

    List<MediaLibrary> findByOrganizationIdAndArchivedIn(Long organizationId, List<Boolean> archivedValues);

	List<MediaLibrary> findByCreatedBy(Long userId);
}