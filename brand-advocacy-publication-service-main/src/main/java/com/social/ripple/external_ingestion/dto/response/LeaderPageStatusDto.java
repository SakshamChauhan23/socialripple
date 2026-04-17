package com.social.ripple.external_ingestion.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class LeaderPageStatusDto {
    private Long userId;
    private String name;
    private String email;
    private boolean leader;
    private List<LeaderPlatformStatusDto> platforms;
}
