/**
 * Filename: UserRoleRepository.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.social.ripple.usermanagement.dao.model.UserRole;
import com.social.ripple.usermanagement.dao.model.UserRoleId;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

	@Query(value = "SELECT r.name FROM roles r WHERE r.id IN (SELECT role_id FROM user_roles WHERE user_id = :userId)", nativeQuery = true)
	List<String> findRoleNamesByUserId(@Param("userId")
	Long userId);
	
	List<UserRole> findByUserId(Long userId);

	UserRole findFirstByUserIdAndRoleId(Long userId, Long roleId);

	@Query(value = "SELECT ur.userId FROM UserRole ur WHERE ur.userId in(:userIds) and ur.roleId = (SELECT id FROM Role WHERE name = :adminRoleName)")
	List<Long> findAdminUserIdsByUserIds(List<Long> userIds, String adminRoleName);
}
