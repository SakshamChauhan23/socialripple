/**
 * Filename: NotificationServiceImpl.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all intellectual
 * property rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix. Access to this
 * software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure agreements
 * explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use this software
 * internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures, routines,
 * customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted above, no
 * license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the license
 * granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any copies. This
 * software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not publicly
 * available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public performance or
 * display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly prohibited and may
 * be in violation of applicable laws.
 */
package com.social.ripple.notification_service.service.implementation;

import com.social.ripple.notification_service.dao.model.Notification;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.social.ripple.notification_service.dto.request.NotificationRequest;
import com.social.ripple.notification_service.dto.response.Response;
import com.social.ripple.notification_service.service.HttpsConnections;
import com.social.ripple.notification_service.service.NotificationService;
import com.social.ripple.notification_service.util.ConfigKeys;
import com.social.ripple.notification_service.util.constants.AppCache;
import com.social.ripple.notification_service.util.constants.ApplicationConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Pattern HARDCODED_INVITE_URL_PATTERN = Pattern.compile(
            "https?://[^\\s\"'>]+/sign-up\\?token=%s",
            Pattern.CASE_INSENSITIVE
    );

    private final HttpsConnections httpsConnections;

    @Override
    public Response sendNotification(String traceId, NotificationRequest request) {
        if (request.getChannel() == null || request.getChannel().isEmpty()) {
            log.error("[{}]|NOTIFY|SendNotification|MissingChannels|Notification channels are empty", traceId);
            return new Response("Notification channel is required", ApplicationConstants.FAILURE, HttpStatus.BAD_REQUEST.value());
        }

        boolean isAnyChannelSuccessful = false;

        for (String channel : request.getChannel()) {
            try {
                Response response = sendToChannel(traceId, request, channel);
                if (response != null && ApplicationConstants.SUCCESS.equals(response.getStatus())) {
                    log.info("[{}]|NOTIFY|SendNotification|ChannelSuccess|Channel: {} | Message: {}", traceId, channel, response.getMessage());
                    isAnyChannelSuccessful = true;
                } else {
                    log.warn("[{}]|NOTIFY|SendNotification|ChannelFailed|Channel: {} | Message: {}", traceId, channel,
                            response != null ? response.getMessage() : "No response returned");
                }
            } catch (Exception ex) {
                log.error("[{}]|NOTIFY|SendNotification|ChannelException|Channel: {} | {}", traceId, channel, ex.getMessage(), ex);
            }
        }

        if (isAnyChannelSuccessful) {
            return new Response("Notification sent successfully to the user", ApplicationConstants.SUCCESS, HttpStatus.OK.value());
        } else {
            log.error("[{}]|NOTIFY|SendNotification|AllChannelsFailed|Notification failed for all channels", traceId);
            return new Response("Notification failed for all channels", ApplicationConstants.FAILURE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private Response sendToChannel(String traceId, NotificationRequest request, String channel) {
        if (channel == null || channel.isBlank()) {
            log.error("[{}]|NOTIFY|SendToChannel|InvalidChannel|Channel is blank or null", traceId);
            return new Response("Invalid notification channel", ApplicationConstants.FAILURE, HttpStatus.BAD_REQUEST.value());
        }

        try {
            return switch (channel.toUpperCase()) {
                case ApplicationConstants.EMAIL_CHANNEL -> handleEmailNotification(traceId, request);
                case ApplicationConstants.WEBSOCKET_CHANNEL -> handleWebsocketNotification(traceId, request);
                default -> {
                    log.error("[{}]|NOTIFY|SendToChannel|UnsupportedChannel|Channel: {}", traceId, channel);
                    yield new Response("Unsupported notification channel: " + channel,
                            ApplicationConstants.FAILURE, HttpStatus.BAD_REQUEST.value());
                }
            };
        } catch (Exception ex) {
            log.error("[{}]|NOTIFY|SendToChannel|Exception|Channel: {} | {}", traceId, channel, ex.getMessage(), ex);
            return new Response("Exception while processing notification for channel: " + channel,
                    ApplicationConstants.FAILURE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private Response handleEmailNotification(String traceId, NotificationRequest request) {
        String email = request.getEmail();
        if (email == null || email.isBlank()) {
            log.error("[{}]|NOTIFY|EmailNotification|MissingEmail|Email is required for email notification", traceId);
            return new Response("Email address is required", ApplicationConstants.FAILURE, HttpStatus.BAD_REQUEST.value());
        }

        String fromAddress = AppCache.configParameters.get(ConfigKeys.MAIL_ID).getConfigValue();

        if (request.getOtp() != null && !request.getOtp().isBlank()) {
            return sendEmailOtp(traceId, email, request.getOtp(), fromAddress);
        }

        if (request.getInvitationLink() != null && !request.getInvitationLink().isBlank()) {
            return sendInvitationLink(traceId, email, request.getInvitationLink(), fromAddress,
                    request.getRecipientName(), request.getOrganizationName(), request.getInviterName(), request.isReinvite());
        }

        log.warn("[{}]|NOTIFY|EmailNotification|MissingContent|Neither OTP nor invitation link provided", traceId);
        return new Response("No email content provided (OTP or invitation link required)",
                ApplicationConstants.FAILURE, HttpStatus.BAD_REQUEST.value());
    }

    private Response handleWebsocketNotification(String traceId, NotificationRequest request) {
//        if("LOYALTY_POINT_ADDED".equals(request.getNotificationEventType())){
//            String action = "POST".equals(request.getPostShareType())? "posting": "sharing";
//
//            String message = String.format("Loyalty points added for %s in %s. Added points: %d",action,request.getPlatform(),request.getPoints());
//
//            Notification notification = new Notification();
//            notification.setType(request.getNotificationEventType());
//            notification.setUserId(request.getUserId());
//            notification.setData(message);
//        }

        // TODO: Send to websocket

        return new Response("Notification added", ApplicationConstants.SUCCESS, HttpStatus.OK.value());
    }

    private Response sendEmailOtp(String traceId, String email, String otp, String from) {
        try {
            log.debug("[{}]|NOTIFY|SendOtpToEmail|Start|Email: {}", traceId, email);
            String subject = "Your SocialRipple verification code";
            String finalBody = generateOtpHtml(otp);

            boolean sent = httpsConnections.sendSimpleMessage(from, email, subject, finalBody);
            if (sent) {
                log.info("[{}]|NOTIFY|SendOtpToEmail|Success|OTP sent to {}", traceId, email);
                return new Response("Email OTP sent successfully", ApplicationConstants.SUCCESS, HttpStatus.OK.value());
            } else {
                log.error("[{}]|NOTIFY|SendOtpToEmail|Failure|Failed to send OTP to {}", traceId, email);
                return new Response("Failed to send OTP", ApplicationConstants.FAILURE, HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } catch (Exception ex) {
            log.error("[{}]|NOTIFY|SendOtpToEmail|Exception|Email: {} | {}", traceId, email, ex.getMessage(), ex);
            return new Response("Exception while sending OTP", ApplicationConstants.FAILURE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }

    private Response sendInvitationLink(String traceId, String email, String invitationLink, String from,
                                        String recipientName, String organizationName, String inviterName, boolean reinvite) {
        try {
            log.debug("[{}]|NOTIFY|SendInvitationEmail|Start|Email: {} reinvite: {}", traceId, email, reinvite);

            String subjectKey = reinvite ? "REINVITE_MAIL_SUBJECT" : "INVITE_MAIL_SUBJECT";
            String subjectTemplate = AppCache.configParameters.containsKey(subjectKey)
                    ? AppCache.configParameters.get(subjectKey).getConfigValue()
                    : (reinvite ? "Your invitation has been renewed" : "You're invited to join SocialRipple");
            String subject = subjectTemplate
                    .replace("{organization_name}", organizationName != null ? organizationName : "SocialRipple")
                    .replace("{inviter_name}", inviterName != null ? inviterName : "");

            String finalBody = generateInvitationLinkHtml(invitationLink, recipientName, organizationName, inviterName);
            boolean sent = httpsConnections.sendSimpleMessage(from, email, subject, finalBody);
            if (sent) {
                log.info("[{}]|NOTIFY|SendInvitationEmail|Success|Invitation link sent to {}", traceId, email);
                return new Response("Invitation link sent successfully", ApplicationConstants.SUCCESS, HttpStatus.OK.value());
            } else {
                log.error("[{}]|NOTIFY|SendInvitationEmail|Failure|Failed to send invitation to {}", traceId, email);
                return new Response("Failed to send invitation", ApplicationConstants.FAILURE, HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
        } catch (Exception ex) {
            log.error("[{}]|NOTIFY|SendInvitationEmail|Exception|Email: {} | {}", traceId, email, ex.getMessage(), ex);
            return new Response("Exception while sending invitation", ApplicationConstants.FAILURE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
	public String generateOtpHtml(String otp) {
		String htmlTemplate = AppCache.configParameters.get(ConfigKeys.OTP_MAIL_BODY).getConfigValue();
		return String.format(htmlTemplate, otp);
	}
	
	public String generateInvitationLinkHtml(String link, String recipientName, String organizationName, String inviterName) {
		String htmlTemplate = AppCache.configParameters.get(ConfigKeys.INVITATION_LINK_MAIL_BODY).getConfigValue();
		return htmlTemplate
				.replace("{invitation_link}", link != null ? link : "#")
				.replace("{recipient_name}", recipientName != null ? recipientName : "there")
				.replace("{organization_name}", organizationName != null ? organizationName : "SocialRipple")
				.replace("{inviter_name}", inviterName != null ? inviterName : "Your team");
	}
}
