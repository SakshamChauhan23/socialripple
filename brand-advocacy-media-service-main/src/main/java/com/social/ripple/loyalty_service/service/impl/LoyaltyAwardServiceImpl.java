package com.social.ripple.loyalty_service.service.impl;

import java.time.LocalDateTime;
import java.util.Optional;

import com.social.ripple.loyalty_service.dao.model.*;
import com.social.ripple.loyalty_service.dao.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.social.ripple.loyalty_service.dto.response.LoyaltyAwardResponse;
import com.social.ripple.loyalty_service.service.ILoyaltyAwardService;
import com.social.ripple.loyalty_service.util.constants.ResponseCode;
import com.social.ripple.loyalty_service.util.constants.WalletConstants;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LoyaltyAwardServiceImpl implements ILoyaltyAwardService {

	private final LoyaltyCreditConfigRepository creditConfigRepository;
	private final LoyaltyPointRepository pointRepository;
	private final LoyaltyTransactionRepository transactionRepository;
	private final WalletRepository walletRepository;
	private final LeaderboardRepository leaderboardRepository;

	@Autowired
	private UserRepository userRepository;

	public LoyaltyAwardServiceImpl(LoyaltyCreditConfigRepository creditConfigRepository,
			LoyaltyPointRepository pointRepository, LoyaltyTransactionRepository transactionRepository,
			WalletRepository walletRepository, LeaderboardRepository leaderboardRepository) {
		this.creditConfigRepository = creditConfigRepository;
		this.pointRepository = pointRepository;
		this.transactionRepository = transactionRepository;
		this.walletRepository = walletRepository;
		this.leaderboardRepository = leaderboardRepository;
	}

	@Override
	@Transactional
	public LoyaltyAwardResponse awardPoints(Long userId, Long creditId, String traceId) {
		log.info("[{}] | LOYALTY | AwardPoints | Start | userId:{} | creditId:{}", traceId, userId, creditId);

		LoyaltyAwardResponse response = new LoyaltyAwardResponse();

		try {
			if (userId == null || creditId == null) {
				response.setStatus(ResponseCode.LYLT_400);
				response.setMessage("Invalid input for awarding points");
				response.setHttpRespCode(400);
				return response;
			}
			LoyaltyCreditConfig creditConfig = creditConfigRepository.findByCreditId(creditId)
					.orElseThrow(() -> new RuntimeException("No active credit config found for creditId: " + creditId));

			Double pointsToAward = creditConfig.getDefaultPoints();
			Wallet wallet = walletRepository.findByName("Default Wallet")
					.orElseThrow(() -> new RuntimeException("Default Wallet not found"));

			LoyaltyPoint pointRecord = pointRepository.findByUserIdAndWallet(userId, wallet).orElseGet(() -> {
				LoyaltyPoint newRecord = new LoyaltyPoint();
				newRecord.setUserId(userId);
				newRecord.setWallet(wallet);
				newRecord.setPoints(0);
				return newRecord;
			});

			int updatedPoints = pointRecord.getPoints() + pointsToAward.intValue();
			pointRecord.setPoints(updatedPoints);
			pointRecord.setUpdatedAt(LocalDateTime.now());
			pointRepository.save(pointRecord);

			Leaderboard leaderboardRecord = leaderboardRepository.findByUserId(userId).orElseGet(() -> {
				User user = userRepository.findById(userId).get();
				Leaderboard newRecord = new Leaderboard();
				newRecord.setUser(user);
				newRecord.setTenantId(user.getOrganization().getId());
				newRecord.setTotalPoints(0);
				return newRecord;
			});

			leaderboardRecord.setTotalPoints(updatedPoints);
			leaderboardRecord.setLastUpdated(LocalDateTime.now());
			leaderboardRepository.save(leaderboardRecord);

			LoyaltyTransaction transaction = new LoyaltyTransaction();
			transaction.setWallet(wallet);
			transaction.setUserId(userId);
			transaction.setCreditId(creditId); // <-- FIX: set the creditId
			transaction.setTransactionType(WalletConstants.TRANSACTION_TYPE_CREDIT);
			transaction.setChannel(WalletConstants.CHANNEL_MOBILE_APP);
			transaction.setPoint(pointsToAward);
			transaction.setDescription("Awarded for creditId: " + creditId);
			transaction.setCreatedAt(LocalDateTime.now());
			transaction.setExpiryDate(LocalDateTime.now().plusDays(30));
			transactionRepository.save(transaction);

			log.info("[{}] | LOYALTY | Success | userId:{} | awarded:{} | newTotal:{} | walletId:{}", traceId, userId,
					pointsToAward, updatedPoints, wallet.getId());

			response.setStatus(ResponseCode.LYLT_200);
			response.setMessage("Points awarded successfully");
			response.setTotalPointsAwarded(pointsToAward.intValue());
			response.setHttpRespCode(200);
			return response;

		} catch (Exception e) {
			log.error("[{}] | LOYALTY | Error | {}", traceId, e.getMessage(), e);

			response.setStatus(ResponseCode.LYLT_500);
			response.setMessage("Failed to award points: " + e.getMessage());
			response.setHttpRespCode(500);
			return response;
		}
	}

	@Override
	public LoyaltyAwardResponse estimatePoints(Long userId, Long creditId, String traceId) {
		LoyaltyAwardResponse response = new LoyaltyAwardResponse();

		Optional<LoyaltyCreditConfig> creditConfigFetch = creditConfigRepository.findByCreditId(creditId);

        creditConfigFetch.ifPresent(loyaltyCreditConfig -> response.setTotalPointsAwarded(loyaltyCreditConfig.getDefaultPoints().intValue()));

		return response;
	}
}
