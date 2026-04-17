package com.social.ripple.usermanagement.dao.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.social.ripple.usermanagement.dao.UserTeamId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_teams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserTeamId.class)
public class UserTeam {

	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Id
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id", nullable = false)
	private Team team;

	@CreationTimestamp
	@Column(name = "joined_at", nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	@Column(name = "is_active")
	private boolean isActive;

	@Column(length = 50)
	private String role = "MEMBER";
}
