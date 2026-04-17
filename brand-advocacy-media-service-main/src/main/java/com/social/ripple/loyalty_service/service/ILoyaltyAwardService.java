package com.social.ripple.loyalty_service.service;

import com.social.ripple.loyalty_service.dto.response.LoyaltyAwardResponse;

public interface ILoyaltyAwardService {

	LoyaltyAwardResponse awardPoints(Long userId, Long creditId, String traceId);

	LoyaltyAwardResponse estimatePoints(Long userId, Long creditId, String traceId);
}
