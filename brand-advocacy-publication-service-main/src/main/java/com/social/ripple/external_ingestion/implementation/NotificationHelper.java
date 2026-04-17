package com.social.ripple.external_ingestion.implementation;

import com.social.ripple.external_ingestion.dao.model.Notification;
import com.social.ripple.external_ingestion.dao.repository.NotificationRepository;
import com.social.ripple.external_ingestion.config.PublicUrlProperties;
import com.social.ripple.external_ingestion.dto.NotificationRequest;
import com.social.ripple.external_ingestion.util.enumeration.AppCache;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
@Slf4j
public class NotificationHelper {

	@Autowired
	private AppCache appCache;
	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private PublicUrlProperties publicUrlProperties;

	public boolean sendNotification(String traceId, Long userId, Integer awardedPoints, String type, Platform platform, String userName) {
		try {
			// String url = appCache.getConfigParameterValue(traceId, ConfigKeys.NOTIFICATION_SERVICE_BASE_URL);
			String url = publicUrlProperties.getNotificationCallbackUrl();
			NotificationRequest notifReq = new NotificationRequest();
			notifReq.setPoints(awardedPoints);
			notifReq.setNotificationEventType("LOYALTY_POINT_ADDED");
			notifReq.setChannel(Collections.singletonList("WEBSOCKET"));
			notifReq.setPlatform(platform.toString());
			notifReq.setUserId(userId);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("x-trace-id", traceId);

			Notification notificationEntity = handleNotificationSave(traceId, notifReq);

			Map websocketRequestMap = new HashMap();
			websocketRequestMap.put("name",userName);
			websocketRequestMap.put("message","{\"type\":\""+ notifReq.getNotificationEventType()  +"\",\"data\":\""+ notificationEntity.getData() +"\"}");

			HttpEntity<Map> httpEntity = new HttpEntity<>(websocketRequestMap, headers);
			log.info("before call request: {}", httpEntity);
			ResponseEntity<String> resp = restTemplate.postForEntity(url, httpEntity, String.class);

			if (!resp.getStatusCode().is2xxSuccessful()) {
				log.warn("[{}]|NotificationFailed|Response:{}", traceId, resp.getBody());
				return false;
			}

			log.info("[{}]|NotificationSent", traceId);
			return true;

		} catch (Exception e) {
			log.error("[{}]|NotificationError|Email:{}|{}", traceId, e.getMessage());
			return false;
		}
	}


	private Notification handleNotificationSave(String traceId, NotificationRequest request) {
		if("LOYALTY_POINT_ADDED".equals(request.getNotificationEventType())){
			String action = "POST".equals(request.getPostShareType())? "posting": "sharing";

			String message = String.format("Loyalty points added for %s in %s. Added points: %d",action,request.getPlatform(),request.getPoints());

			Notification notification = new Notification();
			notification.setType(request.getNotificationEventType());
			notification.setUserId(request.getUserId());
			notification.setData(message);

			notificationRepository.save(notification);
			return notification;
		}
		return null;
	}

}
