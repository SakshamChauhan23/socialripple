package com.social.ripple.external_ingestion.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LeaderPagesResponse extends BaseResponse {
    private List<LeaderPageStatusDto> leaderPages;
}
