package com.social.ripple.external_ingestion.implementation;

import com.social.ripple.external_ingestion.dao.repository.NotificationRepository;
import com.social.ripple.external_ingestion.dto.request.LoyaltyAwardRequest;
import com.social.ripple.external_ingestion.util.enumeration.AppCache;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class LoyaltyPointHelper {

	@Autowired
	private AppCache appCache;
	@Autowired
	private NotificationRepository notificationRepository;
	@Autowired
	private NotificationHelper notificationHelper;

	@Autowired
	private RestTemplate restTemplate;

	public Integer handleLoyaltyPoints(Long postId, Long userId, String type, Platform platform, String userName) {
		try{
			String apiUrl = "http://localhost:2422/v1/loyalty/award";

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("x-trace-id","call_from_ext1");

			LoyaltyAwardRequest request = new LoyaltyAwardRequest();
			request.setUserId(userId);
			request.setIsShare("SHARE".equals(type));

			org.springframework.http.HttpEntity<LoyaltyAwardRequest> entity = new org.springframework.http.HttpEntity<>(request, headers);

			ResponseEntity<Map> responseEntity = restTemplate.exchange(
					apiUrl,
					HttpMethod.POST,
					entity,
					Map.class
			);

			log.info("loyalty response: {}", responseEntity);
			Map awardResponse =null;
			Integer awardedPoints = 0;
			if(responseEntity.getBody() != null){
				awardResponse = responseEntity.getBody();
				log.info("loyalty response.points: {}", awardResponse.get("totalPointsAwarded"));
				awardedPoints = (int)awardResponse.get("totalPointsAwarded");
			}

			if(awardedPoints >0){
				// Insert to notification table
				notificationHelper.sendNotification("trace123",userId, awardedPoints,type,platform,userName);

			}

			return awardedPoints;

		} catch (Exception e) {
			log.error("Error in handleLoyaltyPoints: {}",e.getMessage());
		}
		return null;
	}

}
