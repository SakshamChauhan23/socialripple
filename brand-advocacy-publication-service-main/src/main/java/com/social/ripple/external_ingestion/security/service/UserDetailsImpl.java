/**
 * Filename: UserDetailsImpl.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual property
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
package com.social.ripple.external_ingestion.security.service;

import com.social.ripple.external_ingestion.constants.ApplicationConstants;
import com.social.ripple.external_ingestion.dao.model.Organization;
import com.social.ripple.external_ingestion.dao.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class UserDetailsImpl implements UserDetails {

	private static final long serialVersionUID = 1L;

	private String username;
	private long userId;
	private Organization organization;
	private String password;
	private boolean isLocked;
	private List<GrantedAuthority> authorities;
	private static String traceId;

	public UserDetailsImpl() {
	}

	public UserDetailsImpl(String username, long userid, String password, boolean isLocked, List<GrantedAuthority> authorities,
			Organization organization) {
		log.info("[{}]|UserDetailsImpl|Constructor|Initializing UserDetails instance",traceId);
		log.debug("[{}]|UserDetailsImpl|Constructor|Params - username: {}, userId: {}, password: {}, isLocked: {}, authorities: {},Organization: {}",
				traceId,username, userid, password, isLocked, authorities, organization);
		this.username = username;
		this.userId = userid;
		this.password = password;
		this.isLocked = isLocked;
		this.authorities = authorities;
		this.organization = organization;
		log.debug("[{}]|UserDetailsImpl|Constructor|Completed");
	}

	public static UserDetailsImpl build(String traceId, User user, String inputUserName, List<GrantedAuthority> authorities) {
		log.info("[{}]|UserDetailsImpl|Build|Initializing UserDetailsImpl from User entity", traceId);
		log.debug("[{}]|UserDetailsImpl|Build|Input - user: {}, inputUserName: {}, authorities: {}", traceId, user, inputUserName, authorities);

		try {
			UserDetailsImpl.traceId = traceId;
			if (user == null) {
				log.error("[{}]|UserDetailsImpl|Build|User object is null", traceId);
				return null;
			}

			if (authorities == null) {
				log.warn("[{}]|UserDetailsImpl|Build|Authorities list is null, assigning empty list", traceId);
				authorities = List.of();
			}

			String userName = user.getEmail();
			if (userName == null || userName.isEmpty()) {
				log.warn("[{}]|UserDetailsImpl|Build|User name is null.", traceId);
			}
			long userId = user.getId();
			String passwordHash = user.getPasswordHash();
			Organization organization = user.getOrganization();
			if (passwordHash == null || passwordHash.isEmpty()) {
				log.warn("[{}]|UserDetailsImpl|Build|User password is null.", traceId);
			}
			boolean lockedFlag = ApplicationConstants.USER_LOCKED_STATUS.equalsIgnoreCase(user.getStatus());

			log.debug(
					"[{}]|UserDetailsImpl|Build|Derived values - username: {}, userId: {}, password: {}, isLocked: {}, authorities: {},Organization: {}",
					userName, userId, passwordHash, lockedFlag, authorities, organization);

			log.info("[{}]|UserDetailsImpl|Build|UserDetailsImpl created successfully", traceId);

			return new UserDetailsImpl(userName, userId, passwordHash, lockedFlag, authorities, organization);

		}
		catch (Exception e) {
			log.error("[{}]|UserDetailsImpl|Build|Exception occurred - {}", traceId, e.getMessage(), e);
			return null;
		}
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		log.info("[{}]|UserDetailsImpl|getAuthorities|Fetching user authorities",traceId);
		log.debug("[{}]|UserDetailsImpl|getAuthorities|Authorities: {}", authorities);
		return authorities;
	}

	@Override
	public String getPassword() {
		log.info("[{}]|UserDetailsImpl|getPassword|Fetching user password",traceId);
		log.debug("[{}]|UserDetailsImpl|getPassword|Password: {}", password,traceId);
		return password;
	}

	@Override
	public String getUsername() {
		log.info("[{}]|UserDetailsImpl|getUsername|Fetching username",traceId);
		log.debug("[{}]|UserDetailsImpl|getUsername|Username: {}", username,traceId);
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		log.info("[{}]|UserDetailsImpl|isAccountNonExpired|Checking if account is non-expired",traceId);
		log.debug("[{}]|UserDetailsImpl|isAccountNonExpired|Returning: true",traceId);
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		log.info("[{}]|UserDetailsImpl|isAccountNonLocked|Checking if account is non-locked",traceId);
		log.debug("[{}]|UserDetailsImpl|isAccountNonLocked|isLocked: {}, returning: {}", isLocked, !isLocked,traceId);
		return !isLocked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		log.info("[{}]|UserDetailsImpl|isCredentialsNonExpired|Checking if credentials are non-expired",traceId);
		log.debug("[{}]|UserDetailsImpl|isCredentialsNonExpired|Returning: true",traceId);
		return true;
	}

	@Override
	public boolean isEnabled() {
		log.info("[{}]|UserDetailsImpl|isEnabled|Checking if account is enabled",traceId);
		log.debug("[{}]|UserDetailsImpl|isEnabled|Returning: true",traceId);
		return true;
	}

	public long getUserId() {
		return userId;
	}

	public Organization getOrganization() {
		return organization;
	}

	@Override
	public boolean equals(Object o) {
		log.info("[{}]|UserDetailsImpl|equals|Checking object equality",traceId);
		log.debug("[{}]|UserDetailsImpl|equals|Comparing this: {} with other: {}", this, o,traceId);
		if (this == o)
			return true;
		if (!(o instanceof UserDetailsImpl that))
			return false;
		boolean result = Objects.equals(username, that.username);
		log.debug("UserDetailsImpl|equals|Result: {}", result);
		return result;
	}

	@Override
	public int hashCode() {
		int hash = Objects.hash(username);
		log.info("[{}]|UserDetailsImpl|hashCode|Generating hash code",traceId);
		log.debug("[{}]|UserDetailsImpl|hashCode|Hash: {}", hash,traceId);
		return hash;
	}
}
