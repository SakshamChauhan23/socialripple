package com.social.ripple.usermanagement.dao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.social.ripple.usermanagement.dao.model.Team;
import com.social.ripple.usermanagement.dao.model.User;
import com.social.ripple.usermanagement.dao.model.UserTeam;
import com.social.ripple.usermanagement.dao.UserTeamId;

@Repository
public interface UserTeamRepository extends JpaRepository<UserTeam, UserTeamId> {

	boolean existsByUserAndTeam(User user, Team team);

	List<UserTeam> findByTeam(Team team);

	List<UserTeam> findAllByUser(User user);

	Optional<UserTeam> findByTeamIdAndUserId(Long teamId, Long userId);

	Optional<UserTeam> findFirstByUserId(Long userId);
}
