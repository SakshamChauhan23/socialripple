/**
 * Filename: NotificationClient.java
 *
 * © Copyright 2024 Quasarix. ALL RIGHTS RESERVED.

 * All rights, title and interest (including all intellectual property rights) in this software and any derivative works based upon or derived from
 * this software belongs exclusively to Quasarix.

 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment,
 * the license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies.

 * This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix.

 * Any use, reproduction, modification, distribution, public performance or display of this software or through the use of this software without the
 * prior, express written consent of Quasarix is strictly prohibited and may be in violation of applicable laws.
 *
 */
package com.social.ripple.usermanagement.service;

import java.util.Arrays;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.social.ripple.usermanagement.dto.NotificationRequest;
import com.social.ripple.usermanagement.dto.response.NotificationResponse;
import com.social.ripple.usermanagement.util.AppCache;
import com.social.ripple.usermanagement.util.ConfigKeys;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class NotificationClient {

    private final RestTemplate restTemplate;
    private final AppCache appCache;

    public NotificationClient(AppCache appCache) {
        this.restTemplate = new RestTemplate();
        this.appCache=appCache;
    }
    public NotificationResponse sendNotification(String traceId, String email, String otp) {
        log.info("[{}]|NOTIFICATION_CLIENT|SEND|START|Email: {}", traceId, email);
        
        try {
        	String baseUrl = appCache.getConfigParameterValue(traceId, ConfigKeys.NOTIFICATION_SERVICE_BASE_URL);
        	if (baseUrl == null) {
        	    log.error("[{}]|NOTIFICATION_CLIENT|ERROR|Missing Notification Service Base URL from config", traceId);
        	    throw new RuntimeException("Notification service base URL not configured.");
        	}
            
            NotificationRequest request = new NotificationRequest();
            request.setOtp(otp);
            request.setEmail(email);
            request.setChannel(Arrays.asList("EMAIL"));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-trace-id", traceId);
            
            HttpEntity<NotificationRequest> httpEntity = new HttpEntity<>(request, headers);
            
            log.info("[{}]|NOTIFICATION_CLIENT|SEND|REQUEST|URL: {}|Email: {}", traceId, baseUrl, email);
            
            ResponseEntity<NotificationResponse> response = restTemplate.exchange(
                baseUrl, 
                HttpMethod.POST, 
                httpEntity, 
                NotificationResponse.class
            );
            
            NotificationResponse notificationResponse = response.getBody();
            
            if (notificationResponse != null && "SUCCESS".equals(notificationResponse.getStatus())) {
                log.info("[{}]|NOTIFICATION_CLIENT|SEND|SUCCESS|Email: {}|Message: {}", 
                         traceId, email, notificationResponse.getMessage());
            } else {
                log.warn("[{}]|NOTIFICATION_CLIENT|SEND|FAILED|Email: {}|Message: {}", 
                         traceId, email, notificationResponse != null ? notificationResponse.getMessage() : "No response");
            }
            
            return notificationResponse;
            
        } catch (Exception e) {
            log.error("[{}]|NOTIFICATION_CLIENT|SEND|ERROR|Email: {}|Error: {}", traceId, email, e.getMessage(), e);
            
            NotificationResponse errorResponse = new NotificationResponse();
            errorResponse.setMessage("Failed to send notification: " + e.getMessage());
            errorResponse.setStatus("FAILURE");
            errorResponse.setHttpRespCode(500);
            return errorResponse;
        }
    }
}
