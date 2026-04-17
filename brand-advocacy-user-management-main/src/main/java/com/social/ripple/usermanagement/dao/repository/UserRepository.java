/**
 * Filename: UserRepository.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
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

import java.util.List;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.social.ripple.usermanagement.dao.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

	@Query("SELECT u FROM User u WHERE u.email = :email")
	User findByEmail(@Param("email") String email);

	User findByEmailAndOrganizationId(String email,Long organizationId);

	boolean existsByEmail(String email);

	@Query(value = """
			    SELECT r.name
			    FROM roles r
			    INNER JOIN user_roles ur ON ur.role_id = r.id
			    WHERE ur.user_id = :userId
			""", nativeQuery = true)
	List<String> findRoleNamesByUserId(@Param("userId") Long userId);

	@Query("SELECT u FROM User u WHERE u.id = :userId AND u.organization.id = :orgId")
	Optional<User> findByIdAndOrganizationId(@Param("userId") Long userId, @Param("orgId") Long orgId);

	List<User> findByOrganizationId(Long organizationId);

	Page<User> findByOrganizationId(Long organizationId, Pageable pageable);

	@Modifying
	@Query("UPDATE User u SET u.isLeader = :isLeader WHERE u.id = :userId")
	int updateLeaderStatus(@Param("userId") Long userId, @Param("isLeader") Boolean isLeader);

	@Query(value = "SELECT " +
			"    ROUND( " +
			"        (COUNT(CASE WHEN u.status = 'ACTIVE' THEN 1 END) * 100.0 / COUNT(*)), " +
			"        2 " +
			"    ) AS active_percentage " +
			"FROM users u where u.organization_id=:tenantId", nativeQuery = true)
    Double fetchEmployeeAdoptionPercentage(Long tenantId);
}
