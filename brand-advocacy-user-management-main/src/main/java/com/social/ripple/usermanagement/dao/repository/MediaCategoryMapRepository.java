/**
 * Filename: MediaCategoryMapRepository.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all
 * intellectual property rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix.
 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the
 * license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies. This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not
 * publicly available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public
 * performance or display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly
 * prohibited and may be in violation of applicable laws.
 */
package com.social.ripple.usermanagement.dao.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.social.ripple.usermanagement.dao.model.MediaCategoryMap;

public interface MediaCategoryMapRepository extends JpaRepository<MediaCategoryMap, Long> {

	@Query("SELECT c.name FROM Category c JOIN MediaCategoryMap m ON c.id = m.categoryId WHERE m.libraryId = :libraryId")
	List<String> findCategoryNamesByLibraryId(@Param("libraryId")
	Long libraryId);

	MediaCategoryMap findByLibraryId(Long libraryId);

	void deleteByLibraryId(Long libraryId);
    // Get all category IDs mapped to a library
    @Query("SELECT m.categoryId FROM MediaCategoryMap m WHERE m.libraryId = :libraryId")
    List<Long> findCategoryIdsByLibraryId(@Param("libraryId") Long libraryId);

    // Get all library IDs mapped to a category
    @Query("SELECT m.libraryId FROM MediaCategoryMap m WHERE m.categoryId = :categoryId")
    List<Long> findLibraryIdsByCategoryId(@Param("categoryId") Long categoryId);

}
