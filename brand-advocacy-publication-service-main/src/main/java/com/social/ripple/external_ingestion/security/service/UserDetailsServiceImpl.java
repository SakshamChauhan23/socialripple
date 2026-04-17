/**
 * Filename: UserDetailsServiceImpl.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
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
package com.social.ripple.external_ingestion.security.service;

import com.social.ripple.external_ingestion.dao.model.User;
import com.social.ripple.external_ingestion.dao.repository.UserRepository;
import com.social.ripple.external_ingestion.dao.repository.UserRoleRepository;
import com.social.ripple.external_ingestion.util.AppCommonValidator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

	private final UserRepository userRepository;
	private final UserRoleRepository userRoleRepository;
	private final AppCommonValidator appCommonValidator;

	@Autowired
	private HttpServletRequest request;

	public UserDetailsServiceImpl(UserRepository userRepository, AppCommonValidator appCommonValidator, UserRoleRepository userRoleRepository) {
		this.userRepository = userRepository;
		this.appCommonValidator = appCommonValidator;
		this.userRoleRepository = userRoleRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String inputUserName) throws UsernameNotFoundException {
		String traceId = appCommonValidator.getOrGenerateRequestId(request);
		log.info("[{}]|CONFIGURATION|UserLookup|Start loading user by email", traceId);
		log.debug("[{}]|CONFIGURATION|UserLookup|Input received: {}", traceId, inputUserName);

		if (appCommonValidator.isNullOrEmpty(traceId, inputUserName, "email")) {
			log.warn("[{}]|CONFIGURATION|Validation|Email is null or empty", traceId);
			throw new UsernameNotFoundException("Email cannot be null or empty.");
		}

		if (!appCommonValidator.isValidEmail(traceId, inputUserName)) {
			log.warn("[{}]|CONFIGURATION|Validation|Invalid email format: {}", traceId, inputUserName);
			throw new UsernameNotFoundException("Invalid email format.");
		}

		try {
			User user = userRepository.findByEmail(inputUserName);

			if (user == null) {
				log.warn("[{}]|CONFIGURATION|UserNotFound|No user found for email: {}", traceId, inputUserName);
				throw new UsernameNotFoundException("User not found for email: " + inputUserName);
			}

			log.info("[{}]|CONFIGURATION|UserFound|User ID: {}, Email: {}, OrgId: {}", traceId, user.getId(), user.getEmail(),
					user.getOrganization() != null ? user.getOrganization().getId() : null);

			List<String> roles = userRoleRepository.findRoleNamesByUserId(user.getId());
			log.info("[{}]|CONFIGURATION|RoleFetch|Roles fetched: {}", traceId, roles);

			List<GrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

			log.info("[{}]|CONFIGURATION|UserLookup|User loaded successfully", traceId);
			log.debug("[{}]|CONFIGURATION|UserLookup|UserDetailsImpl creation with authorities: {}", traceId, authorities);

			return UserDetailsImpl.build(traceId, user, inputUserName, authorities);

		}
		catch (Exception ex) {
			log.error("[{}]|CONFIGURATION|Exception|Unexpected error during user loading: {}. Error: {}", traceId, inputUserName, ex.getMessage(),
					ex);
			throw new UsernameNotFoundException("Internal error while loading user.");
		}
	}
}
