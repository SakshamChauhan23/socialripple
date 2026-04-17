/**
 * Filename: InvitationTokenRepository.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED.
 */
package com.social.ripple.usermanagement.dao.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.social.ripple.usermanagement.dao.model.InvitationToken;
import com.social.ripple.usermanagement.dao.model.User;

/**
 * Repository for managing InvitationToken entities.
 */
public interface InvitationTokenRepository extends JpaRepository<InvitationToken, String> {

	boolean existsByEmailAndUsedFalse(String email);

	Optional<InvitationToken> findByTokenAndEmailAndUsedFalse(String token, String email);

	Optional<InvitationToken> findByTokenAndUsedFalse(String inviteToken);

	@Modifying
	@Transactional
	@Query("UPDATE InvitationToken t SET t.used = true WHERE t.token = :token")
	int markTokenAsUsed(@Param("token") String token);

	@Modifying
	@Transactional
	@Query("UPDATE InvitationToken t SET t.used = true WHERE t.email = :email AND t.used = false")
	int markUsedForEmail(@Param("email") String email);

	public InvitationToken findByToken(String token);


	Optional<InvitationToken> findTopByEmailOrderByCreatedAtDesc(String email);

}
