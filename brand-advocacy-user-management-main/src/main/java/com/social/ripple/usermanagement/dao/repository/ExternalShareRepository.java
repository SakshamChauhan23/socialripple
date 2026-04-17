/**
 * Filename: ExternalShareRepository.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.social.ripple.usermanagement.dao.model.ExternalShare;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ExternalShareRepository extends JpaRepository<ExternalShare, Long> {

	Page<ExternalShare> findByUserIdOrderBySharedAtDesc(long userId, Pageable pageable);

	@Query(value = "SELECT platform, COUNT(*) as count FROM external_shares " +
			"WHERE user_id = :userId GROUP BY platform",
			nativeQuery = true)
	List<Object[]> getPlatformWiseCountNative(@Param("userId") Long userId);

	@Query("SELECT es.postId FROM ExternalShare es WHERE es.userId = :userId")
	List<Long> findPostIdsByUserId(@Param("userId") Long userId);

	@Query("SELECT es.postId,sum(es.point) FROM ExternalShare es WHERE es.userId = :userId group by es.postId")
	List<Object[]> findPostIdsByUserIdWithPoint(@Param("userId") Long userId);

	Page<ExternalShare> findByPostIdOrderBySharedAtDesc(Long postId, Pageable pageable);


	@Query(value = "SELECT COUNT(DISTINCT user_id) AS unique_employees_shared " +
			"FROM external_shares " +
			"WHERE shared_at >= DATE_SUB(NOW(), INTERVAL 1 MONTH) " +
			"AND user_id IS NOT NULL " +
			"AND tenant_id = :tenantId;", nativeQuery = true)
	Integer fetchEmployeeMonthlyParticipation(Long tenantId);


	@Query(value = "SELECT COUNT(*) AS total_shares " +
			"FROM external_shares " +
			"WHERE shared_at >= DATE_FORMAT(NOW(), '%Y-01-01') " +
			"AND user_id IS NOT NULL " +
			"AND tenant_id = :tenantId;", nativeQuery = true)
	Integer fetchEmployeeTotalEngagements(Long tenantId);

	@Query(value = "SELECT \n" +
			"    months.month_unique_name,\n" +
			"    months.year_name,\n" +
			"    months.month_name,\n" +
			"    months.month_number,\n" +
			"    COALESCE(COUNT(DISTINCT es.user_id), 0) AS unique_employees_shared\n" +
			"FROM (\n" +
			"    SELECT DATE_FORMAT(DATE_SUB(DATE_FORMAT(NOW(), '%Y-%m-01'), INTERVAL n MONTH), '%Y-%m') AS month_unique_name,\n" +
			"    DATE_FORMAT(DATE_SUB(DATE_FORMAT(NOW(), '%Y-%m-01'), INTERVAL n MONTH), '%Y') AS year_name,\n" +
			"    DATE_FORMAT(DATE_SUB(DATE_FORMAT(NOW(), '%Y-%m-01'), INTERVAL n MONTH), '%b') AS month_name,\n" +
			"    DATE_FORMAT(DATE_SUB(DATE_FORMAT(NOW(), '%Y-%m-01'), INTERVAL n MONTH), '%m') AS month_number\n" +
			"\t FROM (\n" +
			"        SELECT 0 as n UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5\n" +
			"        UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11\n" +
			"    ) months_list\n" +
			") months\n" +
			"LEFT JOIN external_shares es ON \n" +
			"    DATE_FORMAT(es.shared_at, '%Y-%m') = months.month_unique_name\n" +
			"    AND es.user_id IS NOT NULL\n" +
			"    AND es.tenant_id = :tenantId " +
			"    AND es.shared_at >= DATE_SUB(DATE_FORMAT(NOW(), '%Y-%m-01'), INTERVAL 1 YEAR)\n" +
			"GROUP BY months.month_unique_name, months.year_name,\n" +
			"    months.month_name,\n" +
			"    months.month_number\n" +
			"ORDER BY months.month_unique_name;", nativeQuery = true)
	List<Object[]> fetchMonthWiseEmployeeParticipation(Long tenantId);

	@Query(value = "SELECT \n" +
			"    u.id,\n" +
			"    max(u.name) AS user_name,\n" +
			"    COUNT(es.id) AS total_shares\n" +
			"FROM external_shares es\n" +
			"INNER JOIN users u ON es.user_id = u.id\n" +
			"WHERE es.user_id IS NOT NULL AND es.tenant_id=:tenantId\n" +
			"GROUP BY es.tenant_id,u.id, u.id\n" +
			"ORDER BY total_shares DESC;", nativeQuery = true)
	List<Object[]> fetchTopContributors(Long tenantId);

	@Query(value = "SELECT \n" +
			"    es.post_id,\n" +
			"    COUNT(es.id) AS total_shares,\n" +
			"    SUM(CASE WHEN es.platform = 'FACEBOOK' THEN 1 ELSE 0 END) AS facebook_shares,\n" +
			"    SUM(CASE WHEN es.platform = 'X' THEN 1 ELSE 0 END) AS twitter_shares,\n" +
			"    SUM(CASE WHEN es.platform = 'LINKEDIN' THEN 1 ELSE 0 END) AS linkedin_shares,\n" +
			"    SUM(CASE WHEN es.platform = 'INSTAGRAM' THEN 1 ELSE 0 END) AS instagram_shares\n" +
			"FROM external_shares es\n" +
			"WHERE es.post_id IN (:postIds)\n" +
			"GROUP BY es.post_id", nativeQuery = true)
	List<Object[]> fetchShareCountByPostList(List<Long> postIds);

	List<ExternalShare> findByUserId(Long userId);
}
