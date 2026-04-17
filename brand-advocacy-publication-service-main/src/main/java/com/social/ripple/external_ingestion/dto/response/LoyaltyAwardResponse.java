package com.social.ripple.external_ingestion.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoyaltyAwardResponse extends BaseResponse {
    private Integer totalPointsAwarded;
}
