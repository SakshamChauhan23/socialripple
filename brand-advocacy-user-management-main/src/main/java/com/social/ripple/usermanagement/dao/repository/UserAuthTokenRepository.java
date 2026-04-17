package com.social.ripple.usermanagement.dao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.social.ripple.usermanagement.dao.model.UserAuthToken;
import com.social.ripple.usermanagement.util.enumeration.Platform;

@Repository
public interface UserAuthTokenRepository extends JpaRepository<UserAuthToken, Long> {

    /**
     * Find UserAuthToken by local user ID and enum-based platform
     */
    Optional<UserAuthToken> findByUserIdAndPlatform(Long userId, Platform platform);

    List<UserAuthToken> findByUserIdAndPlatformIsNotNull(Long userId);

    List<UserAuthToken> findByUserId(Long userId);

    List<UserAuthToken> findByUserIdIn(List<Long> userIds);
}
