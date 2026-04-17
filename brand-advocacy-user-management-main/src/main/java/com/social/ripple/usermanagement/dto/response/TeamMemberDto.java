package com.social.ripple.usermanagement.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamMemberDto {
    private Long userId;
    private String name;
    private String email;
    private String role;
    private boolean active;
    private LocalDateTime joinedAt;
}
