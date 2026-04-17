/**
 * Filename: PostRepository.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
 * rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this software
 * is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements explicitly
 * covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use this software
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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.social.ripple.usermanagement.dao.model.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

	Page<Post> findByCreatedByOrderByPlatformCreatedAtDescCreatedAtDesc(long userId, Pageable pageable);

	Page<Post> findByCreatedBy(Long userId, Pageable pageable);

	@Query(value = """
			SELECT p.* FROM posts p
			WHERE p.created_by = :userId
			  AND p.organization_id = :tenantId
			  AND (
			      :categoryId IS NULL
			      OR EXISTS (
			          SELECT 1 FROM post_category_map pcm
			          WHERE pcm.post_id = p.id AND pcm.category_id = :categoryId
			      )
			  )
			  AND (
			      :searchText IS NULL
			      OR LOWER(p.title) LIKE LOWER(CONCAT('%', :searchText, '%'))
			  )
			""", countQuery = """
			SELECT COUNT(*) FROM posts p
			WHERE p.created_by = :userId
			  AND p.organization_id = :tenantId
			  AND (
			      :categoryId IS NULL
			      OR EXISTS (
			          SELECT 1 FROM post_category_map pcm
			          WHERE pcm.post_id = p.id AND pcm.category_id = :categoryId
			      )
			  )
			  AND (
			      :searchText IS NULL
			      OR LOWER(p.title) LIKE LOWER(CONCAT('%', :searchText, '%'))
			  )
			""", nativeQuery = true)
	Page<Post> findFilteredPosts(@Param("userId") long userId, @Param("tenantId") Long tenantId,
			@Param("categoryId") Long categoryId, @Param("searchText") String searchText, Pageable pageable);

	Page<Post> findByCreatedByAndOrganizationId(long userId, Long tenantId, Pageable pageable);

	Page<Post> findByOrganizationIdOrderByPlatformCreatedAtDescCreatedAtDesc(Long tenantId, Pageable pageable);

	Optional<Post> findByIdAndOrganizationId(Long postId, Long tenantId);

	@Query("""
			SELECT p FROM Post p
			WHERE p.organizationId = :tenantId
			  AND p.type = :platform
			  AND (p.sourceType = 'BUSINESS_PAGE' OR p.sourceType IS NULL)
			ORDER BY p.platformCreatedAt DESC, p.createdAt DESC
			""")
	Page<Post> findByOrganizationIdAndTypeOrderByPlatformCreatedAtDescCreatedAtDesc(@Param("tenantId") Long tenantId,
			@Param("platform") String platform,
			Pageable pageable);

	@Query("""
			SELECT p FROM Post p
			WHERE p.organizationId = :tenantId
			  AND p.type = :platform
			  AND (p.sourceType = 'BUSINESS_PAGE' OR p.sourceType IS NULL)
			  AND LOWER(p.content) LIKE LOWER(CONCAT('%', :searchText, '%'))
			ORDER BY p.platformCreatedAt DESC, p.createdAt DESC
			""")
	Page<Post> findByOrganizationIdAndTypeAndContentContainingIgnoreCaseOrderByPlatformCreatedAtDescCreatedAtDesc(
			@Param("tenantId") Long tenantId,
			@Param("platform") String platform,
			@Param("searchText") String searchText,
			Pageable pageable);

	@Query("""
			    SELECT p FROM Post p
			    JOIN PostCategoryMap pcm ON p.id = pcm.postId
			    WHERE p.organizationId = :tenantId AND pcm.categoryId = :categoryId
			    AND p.type = :platform
			    AND (p.sourceType = 'BUSINESS_PAGE' OR p.sourceType IS NULL)
			    ORDER BY p.platformCreatedAt DESC, p.createdAt DESC
			""")
	Page<Post> fetchByOrganizationIdAndTypeAndCategoryIdOrderByPlatformCreatedAtDescCreatedAtDesc(Long tenantId, String platform,Long categoryId, Pageable pageable);

	@Query("""
			    SELECT p FROM Post p
			    JOIN PostCategoryMap pcm ON p.id = pcm.postId
			    WHERE p.organizationId = :tenantId AND pcm.categoryId = :categoryId
			    AND p.type = :platform
			    AND (p.sourceType = 'BUSINESS_PAGE' OR p.sourceType IS NULL)
			    AND p.content like %:searchText%
			    ORDER BY p.platformCreatedAt DESC, p.createdAt DESC
			""")
	Page<Post> fetchByOrganizationIdAndTypeAndCategoryIdAndContentContainingOrderByPlatformCreatedAtDescCreatedAtDesc(Long tenantId, String platform,Long categoryId,String searchText,Pageable pageable);

	@Query("""
			    SELECT p FROM Post p
			    JOIN PostCategoryMap pcm ON p.id = pcm.postId
			    WHERE p.organizationId = :tenantId AND pcm.categoryId = :categoryId
			    ORDER BY p.platformCreatedAt DESC, p.createdAt DESC
			""")
	Page<Post> findByTenantAndCategory(@Param("tenantId") Long tenantId, @Param("categoryId") Long categoryId,
			Pageable pageable);

	@Query("""
			    SELECT p FROM Post p
			    JOIN User u ON p.createdBy = u.id
			    WHERE p.organizationId = :tenantId
			    AND u.isLeader = true
			    AND p.sourceType = 'LEADER_PROFILE'
			    ORDER BY p.platformCreatedAt DESC, p.createdAt DESC
			""")
	Page<Post> findByTenantAndLeader(@Param("tenantId") Long tenantId, Pageable pageable);

	@Query("""
			    SELECT p FROM Post p
			    JOIN User u ON p.createdBy = u.id
			    WHERE p.organizationId = :tenantId
			    AND u.isLeader = true
			    AND p.sourceType = 'LEADER_PROFILE'
			    AND p.content like %:searchText%
			    ORDER BY p.platformCreatedAt DESC, p.createdAt DESC
			""")
	Page<Post> findByTenantAndLeaderAndSearch(@Param("tenantId") Long tenantId, @Param("searchText") String searchText,Pageable pageable);

	@Query("""
			    SELECT p FROM Post p
			    JOIN User u ON p.createdBy = u.id
			    JOIN PostCategoryMap pcm ON p.id = pcm.postId
			    WHERE p.organizationId = :tenantId
			    AND u.isLeader = true
			    AND p.sourceType = 'LEADER_PROFILE'
			    AND pcm.categoryId = :categoryId
			    ORDER BY p.platformCreatedAt DESC, p.createdAt DESC
			""")
	Page<Post> fetchByTenantAndLeaderAndCategory(@Param("tenantId") Long tenantId, @Param("categoryId") Long categoryId, Pageable pageable);

	@Query("""
			    SELECT p FROM Post p
			    JOIN User u ON p.createdBy = u.id
			    JOIN PostCategoryMap pcm ON p.id = pcm.postId
			    WHERE p.organizationId = :tenantId
			    AND u.isLeader = true
			    AND p.sourceType = 'LEADER_PROFILE'
			    AND p.content like %:searchText%
			    AND pcm.categoryId = :categoryId
			    ORDER BY p.platformCreatedAt DESC, p.createdAt DESC
			""")
	Page<Post> fetchByTenantAndLeaderAndCategoryAndSearch(@Param("tenantId") Long tenantId, @Param("categoryId") Long categoryId, @Param("searchText") String searchText, Pageable pageable);

	@Query("""
			    SELECT p FROM Post p
			    WHERE p.organizationId = :tenantId
			      AND (p.title LIKE CONCAT('%', :searchText, '%')
			           OR LOWER(p.title) LIKE LOWER(CONCAT('%', :searchText, '%')))
			    ORDER BY p.platformCreatedAt DESC, p.createdAt DESC
			""")
	Page<Post> findByTenantAndSearch(@Param("tenantId") Long tenantId, @Param("searchText") String searchText,
			Pageable pageable);

	Page<Post> findByOrganizationIdAndCreatedByOrderByPlatformCreatedAtDescCreatedAtDesc(Long tenantId,Long userId, Pageable pageable);

	Page<Post> findByOrganizationIdAndCreatedByAndSourceTypeIsNullOrderByCreatedAtDesc(Long tenantId, Long userId, Pageable pageable);

	Page<Post> findByIdInAndOrganizationIdAndCreatedByIsNotOrderByPlatformCreatedAtDescCreatedAtDesc(Collection<Long> postIds, Long tenantId, Long userId, Pageable pageable);

	Page<Post> findByIdInAndOrganizationIdOrderByPlatformCreatedAtDescCreatedAtDesc(Collection<Long> postIds, Long tenantId, Pageable pageable);

	List<Post> findByCreatedBy(Long userId);

	@Query(value = """
			SELECT p.* FROM posts p
			WHERE p.organization_id = :tenantId
			  AND p.is_featured = 1
			  AND (p.featured_until IS NULL OR p.featured_until > NOW())
			ORDER BY p.featured_until ASC
			""", nativeQuery = true)
	List<Post> findActiveFeaturedPosts(@Param("tenantId") Long tenantId);
}
