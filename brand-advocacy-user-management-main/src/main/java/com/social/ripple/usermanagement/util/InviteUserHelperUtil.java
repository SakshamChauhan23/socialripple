package com.social.ripple.usermanagement.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.social.ripple.usermanagement.config.PublicUrlProperties;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.social.ripple.usermanagement.dao.model.InvitationToken;
import com.social.ripple.usermanagement.dao.model.Organization;
import com.social.ripple.usermanagement.dao.model.Role;
import com.social.ripple.usermanagement.dao.model.User;
import com.social.ripple.usermanagement.dao.model.UserRole;
import com.social.ripple.usermanagement.dao.repository.InvitationTokenRepository;
import com.social.ripple.usermanagement.dao.repository.MediaFileRepository;
import com.social.ripple.usermanagement.dao.repository.RoleRepository;
import com.social.ripple.usermanagement.dao.repository.UserRepository;
import com.social.ripple.usermanagement.dao.repository.UserRoleRepository;
import com.social.ripple.usermanagement.dto.NotificationRequest;
import com.social.ripple.usermanagement.dto.request.InviteUserRequest;
import com.social.ripple.usermanagement.dto.response.ErrorObj;
import com.social.ripple.usermanagement.dto.response.InviteUserResponse;
import com.social.ripple.usermanagement.util.constants.ApplicationConstants;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class InviteUserHelperUtil {

	private final UserRepository userRepository;
	private final RoleRepository roleRepository;
	private final UserRoleRepository userRoleRepository;
	private final InvitationTokenRepository invitationTokenRepository;
	private final MediaFileRepository mediaFileRepository;
	private final AppCache appCache;
	private final RestTemplate restTemplate;
	private final PublicUrlProperties publicUrlProperties;

	public InviteUserHelperUtil(UserRepository userRepository, RoleRepository roleRepository,
			UserRoleRepository userRoleRepository, InvitationTokenRepository invitationTokenRepository,
			MediaFileRepository mediaFileRepository, AppCache appCache, RestTemplate restTemplate,
			PublicUrlProperties publicUrlProperties) {
		this.userRepository = userRepository;
		this.roleRepository = roleRepository;
		this.userRoleRepository = userRoleRepository;
		this.invitationTokenRepository = invitationTokenRepository;
		this.mediaFileRepository = mediaFileRepository;
		this.appCache = appCache;
		this.restTemplate = restTemplate;
		this.publicUrlProperties = publicUrlProperties;
	}

	public String generateInviteToken(String traceId, String email) {
		try {
			return UUID.randomUUID().toString();
		} catch (Exception e) {
			log.error("[{}]|TokenGenerationError|Email:{}|{}", traceId, email, e.getMessage(), e);
			return null;
		}
	}

	public boolean sendNotification(String traceId, String email, String phone, String token, boolean isReinvite) {
		return sendNotification(traceId, email, phone, token, isReinvite, null, null, null);
	}

	public boolean sendNotification(String traceId, String email, String phone, String token, boolean isReinvite,
			String recipientName, String organizationName, String inviterName) {
		try {
			String url = appCache.getConfigParameterValue(traceId, ConfigKeys.NOTIFICATION_SERVICE_BASE_URL);
			NotificationRequest notifReq = new NotificationRequest();
			notifReq.setEmail(email);
			notifReq.setPhoneNumber(phone);
			notifReq.setInvitationLink(buildInvitationLink(token));
			notifReq.setReinvite(isReinvite);
			notifReq.setChannel(Collections.singletonList("EMAIL"));
			notifReq.setRecipientName(recipientName);
			notifReq.setOrganizationName(organizationName);
			notifReq.setInviterName(inviterName);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("x-trace-id", traceId);

			HttpEntity<NotificationRequest> httpEntity = new HttpEntity<>(notifReq, headers);
			ResponseEntity<String> resp = restTemplate.postForEntity(url, httpEntity, String.class);

			if (!resp.getStatusCode().is2xxSuccessful()) {
				log.warn("[{}]|NotificationFailed|Email:{}|Response:{}", traceId, email, resp.getBody());
				return false;
			}

			log.info("[{}]|NotificationSent|Email:{}", traceId, email);
			return true;

		} catch (Exception e) {
			log.error("[{}]|NotificationError|Email:{}|{}", traceId, email, e.getMessage());
			return false;
		}
	}

	private String buildInvitationLink(String token) {
		if (token == null || token.isBlank()) {
			return token;
		}
		if (looksLikeAbsoluteUrl(token)) {
			return token;
		}
		String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
		return publicUrlProperties.getFrontendBaseUrl() + "/sign-up?token=" + encodedToken;
	}

	private boolean looksLikeAbsoluteUrl(String value) {
		String normalized = value.trim().toLowerCase();
		return normalized.startsWith("http://") || normalized.startsWith("https://");
	}

	public User createNewUser(InviteUserRequest req, Organization org) {
		User user = new User();
		user.setName(req.getName());
		user.setEmail(req.getEmail());
		user.setPhone(req.getMobileNumber());
		user.setDepartment(req.getDepartment());
		user.setJobTitle(req.getDepartment());
		user.setStatus(ApplicationConstants.USER_INVITED_STATUS);
		user.setOrganization(org);

		if (req.getProfilePictureId() != null) {
			mediaFileRepository.findById(req.getProfilePictureId()).ifPresentOrElse(mediaFile -> {
				user.setProfilePictureUrl(mediaFile.getFileUrl());
			}, () -> {
				throw new IllegalArgumentException("Invalid profile picture id: " + req.getProfilePictureId());
			});
		} else {
			user.setProfilePictureUrl(null);
		}

		return userRepository.save(user);
	}

	public boolean assignUserRoles(User user, List<String> roles, String traceId, InviteUserResponse resp) {
		for (String roleName : roles) {
			Optional<Role> roleOpt = roleRepository.findByName(roleName);
			if (roleOpt.isEmpty()) {
				resp.setStatus(false);
				resp.setCode(ResponseCode.USMG_400);
				resp.setMessage("Invalid role");
				resp.setDevMessage("Role not found: " + roleName);
				resp.setErrors(Collections.singletonList(new ErrorObj("/invite-user", ResponseCode.USMG_400,
						"Invalid role", "Role not found: " + roleName)));
				return false;
			}
			UserRole userRole = new UserRole();
			userRole.setUserId(user.getId());
			userRole.setRoleId(roleOpt.get().getId());
			userRole.setAssignedAt(LocalDateTime.now());
			userRoleRepository.save(userRole);
		}
		return true;
	}

	public InvitationToken saveInvitationToken(String traceId, String token, User user, Organization org,
			String roleName) {
		Optional<Role> roleOpt = roleRepository.findByName(roleName);
		if (roleOpt.isEmpty()) {
			log.warn("[{}]|RoleNotFound|Role:{}", traceId, roleName);
			return null;
		}

		long ttl = 24;
		try {
			String ttlStr = appCache.getConfigParameterValue(traceId, ConfigKeys.INVITE_TOKEN_TTL_HOURS);
			ttl = Long.parseLong(ttlStr);
		} catch (Exception e) {
			log.warn("[{}]|ConfigFallback|Using default TTL=24", traceId);
		}

		invitationTokenRepository.markUsedForEmail(user.getEmail());

		InvitationToken inviteToken = new InvitationToken();
		inviteToken.setToken(token);
		inviteToken.setEmail(user.getEmail().trim().replaceAll("[\\s\\u00A0\\u200B]+", ""));
		inviteToken.setOrganization(org);
		inviteToken.setRoleId(roleOpt.get().getId());
		inviteToken.setCreatedAt(LocalDateTime.now());
		inviteToken.setExpiresAt(LocalDateTime.now().plusHours(ttl));
		inviteToken.setUsed(false);

		return invitationTokenRepository.save(inviteToken);
	}

	public ResponseEntity<InviteUserResponse> buildErrorResponse(String path, String code, String message,
			String devMessage) {
		InviteUserResponse resp = new InviteUserResponse();
		resp.setStatus(false);
		resp.setCode(code);
		resp.setMessage(message);
		resp.setDevMessage(devMessage);
		resp.setErrors(Collections.singletonList(new ErrorObj(path, code, message, devMessage)));
		resp.setTimestamp(new Date());
		return ResponseEntity.status(getHttpStatusFromCode(code)).body(resp);
	}

	public ResponseEntity<BaseResponse> buildBaseErrorResponse(String path, String code, String message,
														   String devMessage) {
		BaseResponse resp = new BaseResponse();
		resp.setStatus(false);
		resp.setCode(code);
		resp.setMessage(message);
		resp.setDevMessage(devMessage);
		resp.setErrors(Collections.singletonList(new ErrorObj(path, code, message, devMessage)));
		resp.setTimestamp(new Date());
		return ResponseEntity.status(getHttpStatusFromCode(code)).body(resp);
	}

	public ResponseEntity<InviteUserResponse> buildSuccessResponse(User user, String token, String message,
			String devMessage) {
		InviteUserResponse resp = new InviteUserResponse();
		resp.setStatus(true);
		resp.setCode(ResponseCode.USMG_200);
		resp.setMessage(message);
		resp.setDevMessage(devMessage);
		resp.setUserId(String.valueOf(user.getId()));
		resp.setEmail(user.getEmail());
		resp.setInvitationToken(token);
		resp.setTimestamp(new Date());
		return ResponseEntity.ok(resp);
	}

	public ResponseEntity<BaseResponse> buildBaseSuccessResponse(User user, String token, String message,
																   String devMessage) {
		BaseResponse resp = new BaseResponse();
		resp.setStatus(true);
		resp.setCode(ResponseCode.USMG_200);
		resp.setMessage(message);
		resp.setDevMessage(devMessage);
		resp.setTimestamp(new Date());
		return ResponseEntity.ok(resp);
	}

	private org.springframework.http.HttpStatus getHttpStatusFromCode(String code) {
		switch (code) {
		case ResponseCode.USMG_400:
			return org.springframework.http.HttpStatus.BAD_REQUEST;
		case ResponseCode.USMG_404:
			return org.springframework.http.HttpStatus.NOT_FOUND;
		case ResponseCode.USMG_409:
			return org.springframework.http.HttpStatus.CONFLICT;
		case ResponseCode.USMG_500:
			return org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
		default:
			return org.springframework.http.HttpStatus.BAD_REQUEST;
		}
	}
}
