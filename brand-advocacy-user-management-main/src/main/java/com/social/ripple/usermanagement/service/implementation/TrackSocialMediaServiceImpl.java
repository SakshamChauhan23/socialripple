/**
 * Filename: TrackSocialMediaServiceImpl.java © Copyright 2024 Quasarix. ALL RIGHTS RESERVED. All rights, title and interest (including all
 * intellectual property rights) in this software and any derivative works based upon or derived from this software belongs exclusively to Quasarix.
 * Access to this software is forbidden to anyone except current employees of Quasarix and its affiliated companies who have executed non-disclosure
 * agreements explicitly covering such access. While in the employment of Quasarix or its affiliate companies as the case may be, employees may use
 * this software internally, solely in the course of employment, for the sole purpose of developing new functionalities, features, procedures,
 * routines, customizations or derivative works, or for the purpose of providing maintenance or support for the software. Save as expressly permitted
 * above, no license or right thereto is hereby granted to anyone, either directly, by implication or otherwise. On the termination of employment, the
 * license granted to employee to access the software shall terminate and the software should be returned to the employer, without retaining any
 * copies. This software is (i) proprietary to Quasarix; (ii) is of significant value to it; (iii) contains trade secrets of Quasarix; (iv) is not
 * publicly available; and (v) constitutes the confidential information of Quasarix. Any use, reproduction, modification, distribution, public
 * performance or display of this software or through the use of this software without the prior, express written consent of Quasarix is strictly
 * prohibited and may be in violation of applicable laws.
 */
package com.social.ripple.usermanagement.service.implementation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.social.ripple.usermanagement.dao.model.Organization;
import com.social.ripple.usermanagement.dao.model.User;
import com.social.ripple.usermanagement.dao.model.UserAuthToken;
import com.social.ripple.usermanagement.dao.model.ExternalPlatform;
import com.social.ripple.usermanagement.dao.repository.ExternalPlatformRepository;
import com.social.ripple.usermanagement.dao.repository.OrganizationRepository;
import com.social.ripple.usermanagement.dao.repository.UserAuthTokenRepository;
import com.social.ripple.usermanagement.dto.BusinessPageInfoDto;
import com.social.ripple.usermanagement.dto.BusinessPageLinksResponse;
import com.social.ripple.usermanagement.dao.repository.UserRepository;
import com.social.ripple.usermanagement.dto.NotificationRequest;
import com.social.ripple.usermanagement.dto.TrackSocialMediaDto;
import com.social.ripple.usermanagement.dto.TrackSocialMediaResponse;
import com.social.ripple.usermanagement.dto.response.BaseResponse;
import com.social.ripple.usermanagement.dto.response.NotificationResponse;
import com.social.ripple.usermanagement.security.service.UserDetailsImpl;
import com.social.ripple.usermanagement.service.ITrackSocialMediaService;
import com.social.ripple.usermanagement.util.AppCache;
import com.social.ripple.usermanagement.util.ConfigKeys;
import com.social.ripple.usermanagement.util.constants.ResponseCode;

import lombok.extern.slf4j.Slf4j;
@Service
@Slf4j
public class TrackSocialMediaServiceImpl implements ITrackSocialMediaService {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final HttpClient BUSINESS_LINK_HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
	private final UserAuthTokenRepository userAuthTokenRepository;
	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;
	private final ExternalPlatformRepository externalPlatformRepository;
	private final RestTemplate restTemplate;
	private AppCache appCache;
	public TrackSocialMediaServiceImpl(UserAuthTokenRepository userAuthTokenRepository, UserRepository userRepository,
			OrganizationRepository organizationRepository, ExternalPlatformRepository externalPlatformRepository, AppCache appCache,
			RestTemplate restTemplate) {
		this.userAuthTokenRepository = userAuthTokenRepository;
		this.userRepository = userRepository;
		this.organizationRepository = organizationRepository;
		this.externalPlatformRepository = externalPlatformRepository;
		this.appCache = appCache;
		this.restTemplate = restTemplate;
	}

	@Override
	public BaseResponse trackSocialMediaConnection(String traceId, String tenantId, UserDetailsImpl userDetails, Long userId, boolean isSelf) {
		Date timestamp = new Date();
		log.info("[{}]|SOCIAL|FETCH|Fetching social media connections for userId={} tenant={}", traceId, userId, tenantId);

		try {
			if (userDetails == null) {
				return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request", "User details missing", timestamp, null);
			}

			Long orgId = userDetails.getOrganization().getId();
			if (tenantId == null || !tenantId.equals(String.valueOf(orgId))) {
				return buildErrorResponse(traceId, ResponseCode.USMG_401, "Tenant/Organization mismatch", null, timestamp, null);
			}

			if (!isSelf && userId == null) {
				return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request", "UserId is required", timestamp, null);
			}
			List<UserAuthToken> authTokens = userAuthTokenRepository.findByUserIdAndPlatformIsNotNull(isSelf?userDetails.getUserId():userId);
			Organization organization = loadOrganization(userDetails.getOrganization().getId());
			Map<String, String> businessLinkByPlatform = resolveBusinessPageLinks(organization.getId());
			if (authTokens == null || authTokens.isEmpty()) {
				TrackSocialMediaResponse emptyResponse = new TrackSocialMediaResponse();
				emptyResponse.setStatus(true);
				emptyResponse.setCode(ResponseCode.USMG_200);
				emptyResponse.setMessage("No social media connections found for user");
				emptyResponse.setTimestamp(timestamp);
				emptyResponse.setSocialMediaConnectionData(Collections.emptyList());
				return emptyResponse;
			}

			List<TrackSocialMediaDto> dtoList = new ArrayList<>();
			for (UserAuthToken token : authTokens) {
				TrackSocialMediaDto dto = new TrackSocialMediaDto();
				dto.setPlatform(token.getPlatform().toString());
				dto.setStatus(token.getStatus());
				dto.setConnectedAt(token.getCreatedAt() != null ? token.getCreatedAt().toString() : null);
				dto.setBusinessPageLink(resolveBusinessPageLink(businessLinkByPlatform, token.getPlatform().toString()));
				dto.setOauthSourcePage(token.getOauthSourcePage());
				dtoList.add(dto);

				log.debug("[{}]|SOCIAL|FETCHED|Platform={} Status={}", traceId, token.getPlatform(), token.getStatus());
			}

			TrackSocialMediaResponse successResponse = new TrackSocialMediaResponse();
			successResponse.setStatus(true);
			successResponse.setCode(ResponseCode.USMG_200);
			successResponse.setMessage("Fetched social media connections successfully");
			successResponse.setTimestamp(timestamp);
			successResponse.setSocialMediaConnectionData(dtoList);

			return successResponse;
		}
		catch (Exception e) {
			log.error("[{}]|SOCIAL|ERROR|{}", traceId, e.getMessage(), e);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Internal server error", e.getMessage(), timestamp, null);
		}
	}

	@Override
	public BaseResponse getBusinessPageLinks(String traceId, String tenantId, UserDetailsImpl userDetails) {
		Date timestamp = new Date();
		log.info("[{}]|SOCIAL|BUSINESS_LINKS|Fetching business page links for tenant={}", traceId, tenantId);

		try {
			if (userDetails == null || userDetails.getOrganization() == null) {
				return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request", "User details missing", timestamp, null);
			}

			Long orgId = userDetails.getOrganization().getId();
			if (tenantId == null || !tenantId.equals(String.valueOf(orgId))) {
				return buildErrorResponse(traceId, ResponseCode.USMG_401, "Tenant/Organization mismatch", null, timestamp, null);
			}

			Map<String, BusinessPageInfoDto> businessPages = buildBusinessPageInfoMap(orgId);
			Map<String, String> normalizedLinks = extractBusinessPageLinks(businessPages);
			BusinessPageLinksResponse response = new BusinessPageLinksResponse();
			response.setStatus(true);
			response.setCode(ResponseCode.USMG_200);
			response.setMessage("Business page links fetched successfully");
			response.setTimestamp(timestamp);
			response.setBusinessPageLinks(normalizedLinks);
			response.setBusinessPages(businessPages);
			return response;
		} catch (Exception ex) {
			log.error("[{}]|SOCIAL|BUSINESS_LINKS|ERROR|{}", traceId, ex.getMessage(), ex);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Internal server error", ex.getMessage(), timestamp, null);
		}
	}

	private BaseResponse buildErrorResponse(String traceId, String code, String message, String path, Date timestamp, String devMessage) {
		BaseResponse response = new BaseResponse();
		response.setStatus(false);
		response.setCode(code);
		response.setMessage(message);
		response.setDevMessage(devMessage);
		response.setTimestamp(timestamp);
		return response;
	}

	private Organization loadOrganization(Long organizationId) {
		return organizationRepository.findById(organizationId).orElseThrow(() -> new IllegalStateException("Organization not found: " + organizationId));
	}

	private Map<String, String> resolveBusinessPageLinks(Long organizationId) {
		Map<String, String> businessLinks = new HashMap<>();
		buildBusinessPageInfoMap(organizationId).forEach((platform, info) -> {
			String normalizedPlatform = platform == null ? null : platform.toUpperCase();
			if (normalizedPlatform != null) {
				if ("TWITTER".equals(normalizedPlatform)) {
					normalizedPlatform = "X";
				}
				businessLinks.put(normalizedPlatform, normalizeLinkValue(info.getUrl()));
			}
		});
		return businessLinks;
	}

	private Map<String, BusinessPageInfoDto> buildBusinessPageInfoMap(Long organizationId) {
		Map<String, BusinessPageInfoDto> businessPages = new LinkedHashMap<>();
		if (organizationId == null) {
			return businessPages;
		}

		for (ExternalPlatform platformRecord : externalPlatformRepository.findByOrganizationId(organizationId)) {
			String platform = normalizePlatformName(platformRecord.getPlatformName());
			if (platform == null) {
				continue;
			}
			String platformKey = "X".equals(platform) ? "x" : platform.toLowerCase();

			ObjectNode credentials = parseSocialHandleConfig(platformRecord.getCredentials());
			String username = firstAvailable(credentials, "username", "screenName", "handle", "userName");
			String pageId = firstAvailable(credentials, "pageId", "id");
			String externalUserId = firstAvailable(credentials, "externalUserId", "userId");
			String url = firstAvailable(credentials, "businessPageLink", "business_page_link", "pageUrl", "profileUrl", "url", "pageLink", "link");
			if (url == null || url.isBlank()) {
				url = derivePageUrl(platform, username, pageId, externalUserId);
			}

			BusinessPageInfoDto dto = new BusinessPageInfoDto();
			dto.setUrl(normalizeLinkValue(url));
			dto.setUsername(username == null ? "" : username.trim());
			dto.setTagText(resolveTagText(platform, dto.getUsername(), dto.getUrl()));
			businessPages.put(platformKey, dto);
		}
		return businessPages;
	}

	private Map<String, String> extractBusinessPageLinks(Map<String, BusinessPageInfoDto> businessPages) {
		Map<String, String> businessLinks = new LinkedHashMap<>();
		businessLinks.put("facebook", "");
		businessLinks.put("instagram", "");
		businessLinks.put("linkedin", "");
		businessLinks.put("x", "");
		businessPages.forEach((platform, info) -> businessLinks.put(platform, info == null ? "" : normalizeLinkValue(info.getUrl())));
		return businessLinks;
	}

	private String resolveTagText(String platform, String username, String url) {
		if (username != null && !username.isBlank() && ("FACEBOOK".equals(platform) || "INSTAGRAM".equals(platform) || "X".equals(platform))) {
			return "@" + username.trim();
		}
		return normalizeLinkValue(url);
	}

	private String normalizePlatformName(String platformName) {
		if (platformName == null || platformName.isBlank()) {
			return null;
		}
		String normalized = platformName.trim().toUpperCase();
		if ("TWITTER".equals(normalized)) {
			return "X";
		}
		return normalized;
	}

	private ObjectNode getOrCreatePlatformNode(ObjectNode rootNode, String platform) {
		String platformKey = "X".equals(platform) ? "twitter" : platform.toLowerCase();
		JsonNode node = rootNode.get(platformKey);
		if (node instanceof ObjectNode objectNode) {
			return objectNode;
		}
		ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
		rootNode.set(platformKey, objectNode);
		return objectNode;
	}

	private void copyIfPresent(ObjectNode source, ObjectNode target, String targetField, String... sourceFields) {
		String value = firstAvailable(source, sourceFields);
		if (value != null && !value.isBlank()) {
			target.put(targetField, value);
		}
	}

	private String firstAvailable(ObjectNode node, String... fieldNames) {
		if (node == null || fieldNames == null) {
			return null;
		}
		for (String fieldName : fieldNames) {
			JsonNode valueNode = node.get(fieldName);
			if (valueNode != null && !valueNode.isNull()) {
				String value = valueNode.asText();
				if (value != null && !value.isBlank()) {
					return value.trim();
				}
			}
		}
		return null;
	}

	private String derivePageUrl(String platform, String username, String pageId, String externalUserId) {
		return switch (platform) {
			case "FACEBOOK" -> pageId != null && !pageId.isBlank() ? "https://www.facebook.com/" + pageId : null;
			case "INSTAGRAM" -> username != null && !username.isBlank() ? "https://www.instagram.com/" + username + "/" : null;
			case "LINKEDIN" -> externalUserId != null && !externalUserId.isBlank() ? "https://www.linkedin.com/company/" + externalUserId : null;
			case "X" -> username != null && !username.isBlank() ? "https://x.com/" + username : null;
			default -> null;
		};
	}

	private Map<String, String> extractBusinessPageLinks(UserDetailsImpl userDetails) {
		Map<String, String> businessLinks = new HashMap<>();
		if (userDetails == null || userDetails.getOrganization() == null) {
			return businessLinks;
		}

		String socialHandleConfig = userDetails.getOrganization().getSocialHandleConfig();
		if (socialHandleConfig == null || socialHandleConfig.isBlank()) {
			return businessLinks;
		}

		try {
			JsonNode root = OBJECT_MAPPER.readTree(socialHandleConfig);
			businessLinks.putAll(extractBusinessPageLinks(root));
		}
		catch (Exception ex) {
			log.warn("Failed to parse social_handle_config for business links: {}", ex.getMessage());
		}
		return businessLinks;
	}

	private Map<String, String> extractBusinessPageLinks(JsonNode root) {
		Map<String, String> businessLinks = new HashMap<>();
		try {
			businessLinks.put("FACEBOOK", extractPlatformLink(root, "facebook"));
			businessLinks.put("INSTAGRAM", extractPlatformLink(root, "instagram"));
			businessLinks.put("LINKEDIN", extractPlatformLink(root, "linkedin"));
			String xLink = extractPlatformLink(root, "x");
			if (xLink == null) {
				xLink = extractPlatformLink(root, "twitter");
			}
			businessLinks.put("X", xLink);
		}
		catch (Exception ex) {
			log.warn("Failed to parse social_handle_config for business links: {}", ex.getMessage());
		}
		return businessLinks;
	}

	private String extractPlatformLink(JsonNode root, String platformKey) {
		if (root == null) {
			return null;
		}
		JsonNode platformNode = root.get(platformKey);
		if (platformNode == null || platformNode.isNull()) {
			return null;
		}
		if (platformNode.isTextual()) {
			return platformNode.asText();
		}
		if (!platformNode.isObject()) {
			return null;
		}
		String[] candidateFields = new String[] { "businessPageLink", "business_page_link", "businessLink", "business_link", "pageLink",
				"page_link", "pageUrl", "page_url", "profileUrl", "profile_url", "url", "link" };
		for (String field : candidateFields) {
			JsonNode valueNode = platformNode.get(field);
			if (valueNode != null && !valueNode.isNull() && valueNode.isTextual() && !valueNode.asText().isBlank()) {
				return valueNode.asText();
			}
		}
		return null;
	}

	private boolean enrichPlatformLink(String traceId, ObjectNode rootNode, Map<String, String> businessLinks, String platform) {
		String existingLink = normalizeLinkValue(businessLinks.get(platform));
		if (!existingLink.isEmpty()) {
			return false;
		}

		ObjectNode platformNode = getPlatformNode(rootNode, platform);
		if (platformNode == null) {
			return false;
		}

		String resolvedLink = switch (platform) {
			case "FACEBOOK" -> resolveFacebookBusinessPageLink(traceId, platformNode);
			case "INSTAGRAM" -> resolveInstagramBusinessPageLink(traceId, platformNode);
			case "LINKEDIN" -> resolveLinkedInBusinessPageLink(platformNode);
			case "X" -> resolveXBusinessPageLink(traceId, platformNode);
			default -> null;
		};

		resolvedLink = normalizeLinkValue(resolvedLink);
		if (resolvedLink.isEmpty()) {
			return false;
		}

		platformNode.put("businessPageLink", resolvedLink);
		businessLinks.put(platform, resolvedLink);
		return true;
	}

	private ObjectNode parseSocialHandleConfig(String socialHandleConfig) {
		if (socialHandleConfig == null || socialHandleConfig.isBlank()) {
			return OBJECT_MAPPER.createObjectNode();
		}

		try {
			JsonNode rootNode = OBJECT_MAPPER.readTree(socialHandleConfig);
			if (rootNode != null && rootNode.isObject()) {
				return (ObjectNode) rootNode;
			}
		}
		catch (Exception ex) {
			log.warn("Failed to parse social_handle_config: {}", ex.getMessage());
		}
		return OBJECT_MAPPER.createObjectNode();
	}

	private ObjectNode getPlatformNode(ObjectNode rootNode, String platform) {
		if (rootNode == null) {
			return null;
		}

		String platformKey = platform.toLowerCase();
		if ("X".equals(platform)) {
			JsonNode xNode = rootNode.get("x");
			if (xNode != null && xNode.isObject()) {
				return (ObjectNode) xNode;
			}
			JsonNode twitterNode = rootNode.get("twitter");
			if (twitterNode != null && twitterNode.isObject()) {
				return (ObjectNode) twitterNode;
			}
			return null;
		}

		JsonNode platformNode = rootNode.get(platformKey);
		if (platformNode != null && platformNode.isObject()) {
			return (ObjectNode) platformNode;
		}
		return null;
	}

	private String resolveFacebookBusinessPageLink(String traceId, ObjectNode platformNode) {
		String pageId = getCandidateField(platformNode, "userId", "pageId", "id");
		String accessToken = getCandidateField(platformNode, "token", "accessToken", "pageAccessToken");
		String graphLink = fetchFacebookPageLink(traceId, pageId, accessToken);
		if (!graphLink.isBlank()) {
			return graphLink;
		}
		return pageId == null || pageId.isBlank() ? null : "https://www.facebook.com/" + pageId;
	}

	private String resolveInstagramBusinessPageLink(String traceId, ObjectNode platformNode) {
		String username = getCandidateField(platformNode, "username", "userName", "handle");
		if (username == null || username.isBlank()) {
			String instagramId = getCandidateField(platformNode, "userId", "instagramUserId", "instagramBusinessId", "id");
			String accessToken = getCandidateField(platformNode, "token", "accessToken", "pageAccessToken");
			username = fetchInstagramUsername(traceId, instagramId, accessToken);
			if (username != null && !username.isBlank()) {
				platformNode.put("username", username);
			}
		}
		return username == null || username.isBlank() ? null : "https://www.instagram.com/" + username + "/";
	}

	private String resolveLinkedInBusinessPageLink(ObjectNode platformNode) {
		String vanityName = getCandidateField(platformNode, "vanityName", "vanity", "organizationHandle");
		if (vanityName != null && !vanityName.isBlank()) {
			return "https://www.linkedin.com/company/" + vanityName;
		}
		String organizationId = getCandidateField(platformNode, "organizationId", "companyId", "userId", "id");
		return organizationId == null || organizationId.isBlank() ? null : "https://www.linkedin.com/company/" + organizationId;
	}

	private String resolveXBusinessPageLink(String traceId, ObjectNode platformNode) {
		String username = getCandidateField(platformNode, "username", "screenName", "handle", "userName");
		if (username == null || username.isBlank()) {
			String userId = getCandidateField(platformNode, "userId", "id");
			String bearerToken = getCandidateField(platformNode, "token", "accessToken", "bearerToken");
			username = fetchXUsername(traceId, userId, bearerToken);
			if (username != null && !username.isBlank()) {
				platformNode.put("username", username);
			}
		}
		if (username != null && !username.isBlank()) {
			return "https://x.com/" + username;
		}
		String userId = getCandidateField(platformNode, "userId", "id");
		return userId == null || userId.isBlank() ? null : "https://x.com/i/user/" + userId;
	}

	private String fetchFacebookPageLink(String traceId, String pageId, String accessToken) {
		if (pageId == null || pageId.isBlank() || accessToken == null || accessToken.isBlank()) {
			return "";
		}

		try {
			JsonNode response = fetchJson("https://graph.facebook.com/v19.0/" + pageId + "?fields=link&access_token=" + accessToken, null);
			JsonNode linkNode = response == null ? null : response.get("link");
			return linkNode != null && !linkNode.isNull() ? linkNode.asText("") : "";
		}
		catch (Exception ex) {
			log.warn("[{}]|SOCIAL|FACEBOOK_LINK|Failed to fetch page link for pageId={}: {}", traceId, pageId, ex.getMessage());
			return "";
		}
	}

	private String fetchInstagramUsername(String traceId, String instagramId, String accessToken) {
		if (instagramId == null || instagramId.isBlank() || accessToken == null || accessToken.isBlank()) {
			return "";
		}

		try {
			JsonNode response = fetchJson("https://graph.facebook.com/v19.0/" + instagramId + "?fields=username&access_token=" + accessToken, null);
			JsonNode usernameNode = response == null ? null : response.get("username");
			return usernameNode != null && !usernameNode.isNull() ? usernameNode.asText("") : "";
		}
		catch (Exception ex) {
			log.warn("[{}]|SOCIAL|INSTAGRAM_LINK|Failed to fetch username for instagramId={}: {}", traceId, instagramId, ex.getMessage());
			return "";
		}
	}

	private String fetchXUsername(String traceId, String userId, String bearerToken) {
		if (userId == null || userId.isBlank() || bearerToken == null || bearerToken.isBlank()) {
			return "";
		}

		try {
			Map<String, String> headers = Map.of("Authorization", "Bearer " + bearerToken);
			JsonNode response = fetchJson("https://api.x.com/2/users/" + userId, headers);
			JsonNode usernameNode = response == null ? null : response.path("data").get("username");
			return usernameNode != null && !usernameNode.isNull() ? usernameNode.asText("") : "";
		}
		catch (Exception ex) {
			log.warn("[{}]|SOCIAL|X_LINK|Failed to fetch username for userId={}: {}", traceId, userId, ex.getMessage());
			return "";
		}
	}

	private JsonNode fetchJson(String url, Map<String, String> headers) throws Exception {
		HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofSeconds(3)).GET();
		if (headers != null) {
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				requestBuilder.header(entry.getKey(), entry.getValue());
			}
		}
		HttpResponse<String> response = BUSINESS_LINK_HTTP_CLIENT.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() >= 200 && response.statusCode() < 300) {
			return OBJECT_MAPPER.readTree(response.body());
		}
		throw new IllegalStateException("HTTP " + response.statusCode() + " for " + url);
	}

	private String getCandidateField(JsonNode node, String... fieldNames) {
		if (node == null || fieldNames == null) {
			return null;
		}
		for (String fieldName : fieldNames) {
			JsonNode valueNode = node.get(fieldName);
			if (valueNode != null && !valueNode.isNull()) {
				String value = valueNode.asText(null);
				if (value != null && !value.isBlank()) {
					return value.trim();
				}
			}
		}
		return null;
	}

	private String resolveBusinessPageLink(Map<String, String> businessLinkByPlatform, String platform) {
		if (businessLinkByPlatform == null || platform == null) {
			return null;
		}
		return businessLinkByPlatform.getOrDefault(platform.toUpperCase(), null);
	}

	private Map<String, String> buildNormalizedBusinessLinkMap(Map<String, String> businessLinkByPlatform) {
		Map<String, String> normalizedLinks = new LinkedHashMap<>();
		normalizedLinks.put("facebook", normalizeLinkValue(businessLinkByPlatform.get("FACEBOOK")));
		normalizedLinks.put("instagram", normalizeLinkValue(businessLinkByPlatform.get("INSTAGRAM")));
		normalizedLinks.put("linkedin", normalizeLinkValue(businessLinkByPlatform.get("LINKEDIN")));
		normalizedLinks.put("x", normalizeLinkValue(businessLinkByPlatform.get("X")));
		return normalizedLinks;
	}

	private Map<String, BusinessPageInfoDto> buildBusinessPageInfoMap(ObjectNode rootNode, Map<String, String> normalizedLinks) {
		Map<String, BusinessPageInfoDto> businessPages = new LinkedHashMap<>();
		businessPages.put("facebook", buildBusinessPageInfo(getPlatformNode(rootNode, "FACEBOOK"), normalizedLinks.get("facebook"), true));
		businessPages.put("instagram", buildBusinessPageInfo(getPlatformNode(rootNode, "INSTAGRAM"), normalizedLinks.get("instagram"), true));
		businessPages.put("linkedin", buildBusinessPageInfo(getPlatformNode(rootNode, "LINKEDIN"), normalizedLinks.get("linkedin"), false));
		businessPages.put("x", buildBusinessPageInfo(getPlatformNode(rootNode, "X"), normalizedLinks.get("x"), true));
		return businessPages;
	}

	private BusinessPageInfoDto buildBusinessPageInfo(ObjectNode platformNode, String url, boolean taggingAllowed) {
		BusinessPageInfoDto info = new BusinessPageInfoDto();
		info.setUrl(normalizeLinkValue(url));
		String username = taggingAllowed ? normalizeLinkValue(getCandidateField(platformNode, "username", "screenName", "handle", "userName")) : "";
		info.setUsername(username);
		info.setTagText(username.isEmpty() ? "" : "@" + username);
		return info;
	}

	private String normalizeLinkValue(String value) {
		return value == null ? "" : value.trim();
	}

	@Override
	public BaseResponse sendSocialMediaRemainder(String traceId, String tenantId, UserDetailsImpl userDetails, Long userId, String platform) {

		Date timestamp = new Date();
		log.info("[{}]|SOCIAL|REMAINDER_INIT|Preparing remainder for userId={} tenant={} platform={}", traceId, userId, tenantId, platform);

		try {
			if (userDetails == null || tenantId == null || userId == null || platform == null) {
				return buildErrorResponse(traceId, ResponseCode.USMG_400, "Invalid request", "/socialmedia/send/remainder/" + userId + "/" + platform,
						timestamp, "Missing required parameters");
			}

			User user = userRepository.findById(userId).orElse(null);
			if (user == null) {
				return buildErrorResponse(traceId, ResponseCode.USMG_404, "User not found", "/socialmedia/send/remainder/" + userId + "/" + platform,
						timestamp, null);
			}

			Map<String, String> platformMapping = new HashMap<>();
			platformMapping.put("X", "TWITTER");
			platformMapping.put("FACEBOOK", "FACEBOOK");
			platformMapping.put("INSTAGRAM", "INSTAGRAM");
			platformMapping.put("LINKEDIN", "LINKEDIN");

			String normalizedPlatform = platformMapping.getOrDefault(platform.toUpperCase(), platform.toUpperCase());

			List<UserAuthToken> userTokens = userAuthTokenRepository.findByUserIdAndPlatformIsNotNull(userId);

			String status = null;
			if (userTokens != null) {
				for (UserAuthToken token : userTokens) {
					String rawPlatform = token.getPlatform().toString();
					String mapped = platformMapping.getOrDefault(rawPlatform, rawPlatform);

					if (mapped.equalsIgnoreCase(normalizedPlatform)) {
						status = token.getStatus();
						break;
					}
				}
			}

			if (status == null) {
				status = "NEVER CONNECTED";
			}

			if (!"EXPIRED".equalsIgnoreCase(status) && !"NEVER CONNECTED".equalsIgnoreCase(status)) {
				log.info("[{}]|SOCIAL|REMAINDER_SKIP|UserId={} platform={} already connected", traceId, userId, normalizedPlatform);
				return buildSuccessResponse(traceId, "Platform " + normalizedPlatform + " is already connected. No reminder needed.", timestamp);
			}

			String baseUrl = appCache.getConfigParameterValue(traceId, ConfigKeys.NOTIFICATION_SERVICE_BASE_URL);
			if (baseUrl == null || baseUrl.isEmpty()) {
				log.error("[{}]|SOCIAL|REMAINDER_FAILED|Notification service URL not configured", traceId);
				return buildErrorResponse(traceId, ResponseCode.USMG_500, "Notification service URL not configured",
						"/socialmedia/send/remainder/" + userId + "/" + platform, timestamp, null);
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("x-trace-id", traceId);
			

			NotificationRequest notifReq = new NotificationRequest();
			notifReq.setEmail(user.getEmail());
			notifReq.setChannel(Collections.singletonList("EMAIL"));
			notifReq.setUserId(user.getId());
			notifReq.setMessage(user.getName());

			if ("EXPIRED".equalsIgnoreCase(status)) {
				notifReq.setExpiredPlatforms(Collections.singletonList(normalizedPlatform));
			}
			else {
				notifReq.setNeverConnectedPlatforms(Collections.singletonList(normalizedPlatform));
			}

			HttpEntity<NotificationRequest> entity = new HttpEntity<>(notifReq, headers);
			restTemplate.exchange(baseUrl, HttpMethod.POST, entity, NotificationResponse.class);

			log.info("[{}]|SOCIAL|REMAINDER_SENT|Email:{}|Platform:{}|Status:{}", traceId, user.getEmail(), normalizedPlatform, status);

			return buildSuccessResponse(traceId, "Reminder sent for platform: " + normalizedPlatform + " (" + status + ")", timestamp);

		}
		catch (Exception e) {
			log.error("[{}]|SOCIAL|REMAINDER_EXCEPTION|Platform:{}|{}", traceId, platform, e.getMessage(), e);
			return buildErrorResponse(traceId, ResponseCode.USMG_500, "Exception while sending remainder",
					"/socialmedia/send/remainder/" + userId + "/" + platform, timestamp, e.getMessage());
		}
	}

	private BaseResponse buildSuccessResponse(String traceId, String message, Date timestamp) {
		BaseResponse response = new BaseResponse();
		response.setStatus(true);
		response.setCode(ResponseCode.USMG_200);
		response.setMessage(message);
		response.setTimestamp(timestamp);
		return response;
	}
}
