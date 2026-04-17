package com.social.ripple.usermanagement.dao.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.social.ripple.usermanagement.dao.model.Team;
import com.social.ripple.usermanagement.dao.model.User;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByTeamName(String teamName);

    @Query("SELECT t FROM Team t WHERE t.createdBy.id = :userId")
    List<Team> findByCreatedById(@Param("userId") Long userId);

    @Query("SELECT t FROM Team t WHERE t.createdBy.organization.id = :orgId")
    List<Team> findByCreatedByOrganizationId(@Param("orgId") Long orgId);
    
    Optional<Team> findByCreatedBy(User user);

}
