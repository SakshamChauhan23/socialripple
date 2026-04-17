package com.social.ripple.loyalty_service.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoyaltyAwardResponse extends Response {
    private Integer totalPointsAwarded;
}
