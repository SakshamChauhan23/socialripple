package com.social.ripple.external_ingestion.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.ripple.external_ingestion.dao.model.*;
import com.social.ripple.external_ingestion.dao.repository.OrganizationRepository;
import com.social.ripple.external_ingestion.dao.repository.PostMediaRepository;
import com.social.ripple.external_ingestion.dao.repository.PostRepository;
import com.social.ripple.external_ingestion.dao.repository.UserAuthTokenRepository;
import com.social.ripple.external_ingestion.dao.repository.UserRepository;
import com.social.ripple.external_ingestion.service.IFbContentFetcherService;
import com.social.ripple.external_ingestion.util.enumeration.AuthStatus;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class FbContentFetcherServiceImpl implements IFbContentFetcherService {
	private static final ObjectMapper mapper = new ObjectMapper();
	private static final String SOURCE_BUSINESS_PAGE = "BUSINESS_PAGE";
	private static final String SOURCE_LEADER_PROFILE = "LEADER_PROFILE";
	private static final int FETCH_LIMIT = 50;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private PostMediaRepository postMediaRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private FetchedMediaStorageService fetchedMediaStorageService;

	@Autowired
	private com.social.ripple.external_ingestion.service.OrganizationPlatformSettingsService organizationPlatformSettingsService;

	@Autowired
	private UserAuthTokenRepository userAuthTokenRepository;

	private static final String DATA_ACCESS_EXPIRED_REASON =
			"Data access expired - user must reconnect this account";

	@Override
	public void fetchFromExternalPlatformFb(Platform platform) {
		log.info("Start fetching Facebook");
		String fields = "id,message,created_time,permalink_url," +
				"attachments%7Btype,media,subattachments%7D";
		List<com.social.ripple.external_ingestion.service.OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig> configs =
				organizationPlatformSettingsService.getEnabledPlatformConfigs(Platform.FACEBOOK);
		for (var config : configs) {
			fetchFacebookBusinessPageConfig(config, fields);
		}

		for (var config : organizationPlatformSettingsService.getConnectedLeaderPlatformConfigs(Platform.FACEBOOK)) {
			String pageId = config.getExternalUserId();
			String token = config.getAccessToken();
			String url = String.format("%s/v24.0/%s/posts?fields=%s&limit=%d&access_token=%s",
					trimTrailingSlash(config.getApiUrl()), pageId, fields, FETCH_LIMIT, token);
			organizationPlatformSettingsService.markLeaderSyncStarted(config.getUserId(), Platform.FACEBOOK);

			// Fetch page name and avatar from FB Graph API
			String fbPageName = null;
			String fbPageAvatar = null;
			try {
				String pageInfoUrl = String.format("%s/v24.0/%s?fields=name,picture&access_token=%s",
						trimTrailingSlash(config.getApiUrl()), pageId, token);
				HttpGet pageInfoReq = new HttpGet(pageInfoUrl);
				pageInfoReq.setHeader("Accept", "application/json");
				try (CloseableHttpClient piClient = HttpClients.createDefault();
					 CloseableHttpResponse piResp = piClient.execute(pageInfoReq)) {
					if (piResp.getStatusLine().getStatusCode() == 200) {
						JsonNode pageInfo = new ObjectMapper().readTree(EntityUtils.toString(piResp.getEntity()));
						fbPageName = pageInfo.path("name").asText(null);
						JsonNode pictureData = pageInfo.path("picture").path("data");
						if (!pictureData.isMissingNode()) {
							fbPageAvatar = pictureData.path("url").asText(null);
						}
					}
				}
			} catch (Exception e) {
				log.warn("facebook-leader|userId={}|Failed to fetch page info for {}: {}", config.getUserId(), pageId, e.getMessage());
			}

			HttpGet request = new HttpGet(url);
			request.setHeader("Accept", "application/json");

			try (CloseableHttpClient client = HttpClients.createDefault();
				 CloseableHttpResponse response = client.execute(request)) {
				int statusCode = response.getStatusLine().getStatusCode();
				String responseBody = EntityUtils.toString(response.getEntity());
				User leader = userRepository.findById(config.getUserId()).orElse(null);

				if (statusCode == 200 && leader != null) {
					String resolvedName = fbPageName != null ? fbPageName : leader.getName();
					String resolvedAvatar = fbPageAvatar != null ? fbPageAvatar : leader.getProfilePictureUrl();
					int newPosts = processFacebookResponse(responseBody, config.getOrganizationId(), leader, SOURCE_LEADER_PROFILE,
							resolvedName, null, resolvedAvatar);
					organizationPlatformSettingsService.markLeaderSyncSuccess(config.getUserId(), Platform.FACEBOOK, newPosts);
				} else if (leader == null) {
					organizationPlatformSettingsService.markLeaderSyncFailure(config.getUserId(), Platform.FACEBOOK, "Leader not found");
				} else if (statusCode != 200) {
					log.error("facebook-leader|userId={}|orgId={}|HTTP {}, {}", config.getUserId(), config.getOrganizationId(), statusCode, responseBody);
					if (isMetaDataAccessExpired(statusCode, responseBody)) {
						markLeaderTokenDisconnected(config.getUserId(), Platform.FACEBOOK, DATA_ACCESS_EXPIRED_REASON);
						organizationPlatformSettingsService.markLeaderSyncFailure(config.getUserId(), Platform.FACEBOOK, DATA_ACCESS_EXPIRED_REASON);
					}
					else {
						organizationPlatformSettingsService.markLeaderSyncFailure(config.getUserId(), Platform.FACEBOOK, syncFailureReason(statusCode));
					}
				}
			} catch (Exception e) {
				log.error("facebook leader fetch failed for userId={} orgId={}", config.getUserId(), config.getOrganizationId(), e);
				organizationPlatformSettingsService.markLeaderSyncFailure(config.getUserId(), Platform.FACEBOOK, syncFailureReason(e));
			}
		}
	}

	@Override
	public void fetchFromExternalPlatformFbForOrganization(Long organizationId) {
		if (organizationId == null) {
			return;
		}

		String fields = "id,message,created_time,permalink_url," +
				"attachments%7Btype,media,subattachments%7D";

		findEnabledOrgConfig(Platform.FACEBOOK, organizationId)
				.ifPresent(config -> fetchFacebookBusinessPageConfig(config, fields));
	}

	private void fetchFacebookBusinessPageConfig(
			com.social.ripple.external_ingestion.service.OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig config,
			String fields
	) {
		String pageId = config.resolvedExternalId();
		String token = config.getAccessToken();
		String url = String.format("%s/v24.0/%s/posts?fields=%s&limit=50&access_token=%s",
				trimTrailingSlash(config.getApiUrl()), pageId, fields, token);
		organizationPlatformSettingsService.markSyncStarted(config.getOrganizationId(), Platform.FACEBOOK);

		// Fetch page avatar
		String pageAvatar = null;
		try {
			String pageInfoUrl = String.format("%s/v24.0/%s?fields=picture&access_token=%s",
					trimTrailingSlash(config.getApiUrl()), pageId, token);
			HttpGet pageInfoReq = new HttpGet(pageInfoUrl);
			pageInfoReq.setHeader("Accept", "application/json");
			try (CloseableHttpClient piClient = HttpClients.createDefault();
				 CloseableHttpResponse piResp = piClient.execute(pageInfoReq)) {
				if (piResp.getStatusLine().getStatusCode() == 200) {
					JsonNode pageInfo = new ObjectMapper().readTree(EntityUtils.toString(piResp.getEntity()));
					JsonNode pictureData = pageInfo.path("picture").path("data");
					if (!pictureData.isMissingNode()) {
						pageAvatar = pictureData.path("url").asText(null);
					}
				}
			}
		} catch (Exception e) {
			log.warn("facebook|orgId={}|Failed to fetch page picture: {}", config.getOrganizationId(), e.getMessage());
		}

		HttpGet request = new HttpGet(url);
		request.setHeader("Accept", "application/json");

		try (CloseableHttpClient client = HttpClients.createDefault();
			 CloseableHttpResponse response = client.execute(request)) {

			int statusCode = response.getStatusLine().getStatusCode();
			String responseBody = EntityUtils.toString(response.getEntity());

			log.info("facebook|orgId={}|responseBody={}", config.getOrganizationId(), responseBody);

			if (statusCode == 200) {
				int newPosts = processFacebookResponse(responseBody, config.getOrganizationId(), getOrganizationPostedUser(config.getOrganizationId()), SOURCE_BUSINESS_PAGE,
							config.getDisplayName(), config.getUsername(), pageAvatar);
				organizationPlatformSettingsService.markSyncSuccess(config.getOrganizationId(), Platform.FACEBOOK, newPosts);
			} else {
				log.error("facebook|orgId={}|HTTP {}, {}", config.getOrganizationId(), statusCode, responseBody);
				String failureReason = isMetaDataAccessExpired(statusCode, responseBody)
						? DATA_ACCESS_EXPIRED_REASON
						: syncFailureReason(statusCode);
				organizationPlatformSettingsService.markSyncFailure(config.getOrganizationId(), Platform.FACEBOOK, failureReason);
			}
		} catch (Exception e) {
			log.error("facebook fetch failed for orgId={}", config.getOrganizationId(), e);
			organizationPlatformSettingsService.markSyncFailure(config.getOrganizationId(), Platform.FACEBOOK, syncFailureReason(e));
		}
	}

	private int processFacebookResponse(String responseBody, Long organizationId, User ownerUser, String sourceType) throws Exception {
		return processFacebookResponse(responseBody, organizationId, ownerUser, sourceType, null, null, null);
	}

	private int processFacebookResponse(String responseBody, Long organizationId, User ownerUser, String sourceType,
										String sourceName, String sourceUsername, String sourceAvatarUrl) throws Exception{
		JsonNode response = mapper.readTree(responseBody);
		int newPosts = 0;

		if (response != null && response.has("data")) {
			JsonNode elements = response.get("data");

			Organization organization = organizationRepository.findById(organizationId).orElseThrow();

			Set<String> existingPostIds = ownerUser == null
					? Set.of()
					: postRepository.findPlatformUniqueIdsByPlatformAndOrganizationAndCreatedBy("FACEBOOK", organizationId, ownerUser.getId());

			List<Post> posts = new ArrayList<>();

			if (elements != null && elements.isArray()) {
				for (JsonNode postNode : elements) {
					if (!existingPostIds.contains(postNode.get("id").asText())) {
						Post post = parseFacebookPostNode(postNode, organization, ownerUser, sourceType, sourceName, sourceUsername, sourceAvatarUrl);
						postRepository.saveAndFlush(post);
						newPosts++;
						// If have media
						if (postNode.has("attachments")) {
							JsonNode attachments = postNode.get("attachments");
							if (attachments.has("data") && attachments.get("data").isArray()) {
								JsonNode entitiesArray = attachments.get("data");

								for (JsonNode dataNode : entitiesArray) {
									String type = dataNode.get("type").asText();
									log.info("type:{}",type);
									if(type!=null && type.equals("video_inline")){
										PostMedia postMedia = parseVideoEntity(dataNode, organization);
										postMedia.setPostId(post.getId());
										postMedia.setMediaType("VIDEO");
										postMediaRepository.save(postMedia);
										log.info("video saved");
									}else if(type!=null && type.contains("video")){
										PostMedia postMedia = parseVideoEntity(dataNode, organization);
										postMedia.setPostId(post.getId());
										postMedia.setMediaType("VIDEO");
										postMediaRepository.save(postMedia);
										log.info("video saved");
									}else if(type!=null){
										PostMedia postMedia = parseMediaEntityWithSrc(dataNode, organization);
										postMedia.setPostId(post.getId());
										postMedia.setMediaType("IMAGE");
										postMediaRepository.save(postMedia);
										log.info("image saved");
									}
								}
							}
						}
						posts.add(post);
					}
				}
			}
		}
		return newPosts;
	}

	private PostMedia parseVideoEntity(JsonNode entityNode, Organization organization) {
		PostMedia entity = new PostMedia();
		String externalUrl = null;
		if(entityNode.has("media")){
			if(entityNode.get("media").has("source")) {
				externalUrl = entityNode.get("media").get("source").asText();
			}else if(entityNode.get("media").has("image")) {
				externalUrl = entityNode.get("media").get("image").get("src").asText();
			}
		}

		applyFetchedMedia(entity, externalUrl, "VIDEO", organization.getId());

		return entity;
	}

	private PostMedia parseMediaEntityWithSrc(JsonNode entityNode, Organization organization) {
		PostMedia entity = new PostMedia();
		if(entityNode.has("media") && entityNode.get("media").has("image")) {
			applyFetchedMedia(entity, entityNode.get("media").get("image").get("src").asText(), "IMAGE", organization.getId());
		}
		return entity;
	}

	private Post parseFacebookPostNode(JsonNode postNode, Organization organization, User ownerUser, String sourceType) {
		return parseFacebookPostNode(postNode, organization, ownerUser, sourceType, null, null, null);
	}

	private Post parseFacebookPostNode(JsonNode postNode, Organization organization, User ownerUser, String sourceType,
									   String sourceName, String sourceUsername, String sourceAvatarUrl) {
		Post post = new Post();

		post.setType("FACEBOOK");
		post.setSourceType(sourceType);
		post.setPlatformUniqueId(postNode.get("id").asText());
		post.setSourceName(sourceName);
		post.setSourceUsername(sourceUsername);
		post.setSourceAvatarUrl(sourceAvatarUrl);

		if (postNode.has("permalink_url")) {
			post.setFbLink(postNode.get("permalink_url").asText());
		}

		if (postNode.has("message")) {
			post.setContent(postNode.get("message").asText());
		}
		post.setStatus("PUBLISHED");

		post.setOrganization(organization);

		if (postNode.has("created_time")) {
			try {
				post.setPlatformCreatedAt(parseDateTime(postNode.get("created_time").asText()));
				post.setCreatedAt(LocalDateTime.now());
			} catch (Exception e) {
				post.setPlatformCreatedAt(LocalDateTime.now());
				post.setCreatedAt(LocalDateTime.now());
			}
		}else {
			post.setPlatformCreatedAt(LocalDateTime.now());
			post.setCreatedAt(LocalDateTime.now());
		}

		if(ownerUser != null){
			post.setCreatedBy(ownerUser);
		}


		return post;
	}

	private Post parseInstagramPostNode(JsonNode postNode, Organization organization, User ownerUser, String sourceType) {
		return parseInstagramPostNode(postNode, organization, ownerUser, sourceType, null, null, null);
	}

	private Post parseInstagramPostNode(JsonNode postNode, Organization organization, User ownerUser, String sourceType,
										String sourceName, String sourceUsername, String sourceAvatarUrl) {
		Post post = new Post();

		post.setType("INSTAGRAM");
		post.setSourceType(sourceType);
		post.setPlatformUniqueId(postNode.get("id").asText());
		post.setSourceName(sourceName);
		post.setSourceUsername(sourceUsername);
		post.setSourceAvatarUrl(sourceAvatarUrl);

		if (postNode.has("permalink")) {
			post.setFbLink(postNode.get("permalink").asText());
		}

		if (postNode.has("caption")) {
			post.setContent(postNode.get("caption").asText());
		}else {
			post.setContent("");
		}
		post.setStatus("PUBLISHED");

		post.setOrganization(organization);

		if (postNode.has("timestamp")) {
			try {
				post.setPlatformCreatedAt(parseDateTime(postNode.get("timestamp").asText()));
				post.setCreatedAt(LocalDateTime.now());
			} catch (Exception e) {
				post.setPlatformCreatedAt(LocalDateTime.now());
				post.setCreatedAt(LocalDateTime.now());
			}
		}else {
			post.setPlatformCreatedAt(LocalDateTime.now());
			post.setCreatedAt(LocalDateTime.now());
		}

		if(ownerUser != null){
			post.setCreatedBy(ownerUser);
		}


		return post;
	}

	private LocalDateTime parseDateTime(String dateTimeStr) {
		if (dateTimeStr == null) return null;
		try {
			// Remove timezone offset for simplicity, or use custom formatter
			String cleaned = dateTimeStr.replace("+0000", "");
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
			return LocalDateTime.parse(cleaned, formatter);
		} catch (Exception e) {
			return null;
		}
	}

	private User getOrganizationPostedUser(Long id) {
		return organizationPlatformSettingsService.findOrganizationPostedUser(id).orElse(null);
	}

	@Override
	public void fetchFromExternalPlatformInstagram(Platform platform) {
		log.info("Start fetching Instagram");
		List<com.social.ripple.external_ingestion.service.OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig> configs =
				organizationPlatformSettingsService.getEnabledPlatformConfigs(Platform.INSTAGRAM);
		for (var config : configs) {
			fetchInstagramBusinessPageConfig(config);
		}

		for (var config : organizationPlatformSettingsService.getConnectedLeaderPlatformConfigs(Platform.INSTAGRAM)) {
			String pageId = config.getExternalUserId();
			String token = config.getAccessToken();
			String url = String.format("%s/v24.0/%s/media?fields=id,caption,media_type,media_url,timestamp,permalink&limit=%d&access_token=%s",
					trimTrailingSlash(config.getApiUrl()), pageId, FETCH_LIMIT, token);
			organizationPlatformSettingsService.markLeaderSyncStarted(config.getUserId(), Platform.INSTAGRAM);

			// Fetch IG account name and profile pic
			String igName = null;
			String igUsername = null;
			String igAvatar = null;
			try {
				String igInfoUrl = String.format("%s/v24.0/%s?fields=name,username,profile_picture_url&access_token=%s",
						trimTrailingSlash(config.getApiUrl()), pageId, token);
				HttpGet igInfoReq = new HttpGet(igInfoUrl);
				igInfoReq.setHeader("Accept", "application/json");
				try (CloseableHttpClient igClient = HttpClients.createDefault();
					 CloseableHttpResponse igResp = igClient.execute(igInfoReq)) {
					if (igResp.getStatusLine().getStatusCode() == 200) {
						JsonNode igInfo = new ObjectMapper().readTree(EntityUtils.toString(igResp.getEntity()));
						igName = igInfo.path("name").asText(null);
						String rawUsername = igInfo.path("username").asText(null);
						if (rawUsername != null && !rawUsername.isEmpty()) {
							igUsername = "@" + rawUsername;
						}
						igAvatar = igInfo.path("profile_picture_url").asText(null);
					}
				}
			} catch (Exception e) {
				log.warn("instagram-leader|userId={}|Failed to fetch IG account info for {}: {}", config.getUserId(), pageId, e.getMessage());
			}

			HttpGet request = new HttpGet(url);
			request.setHeader("Accept", "application/json");

			try (CloseableHttpClient client = HttpClients.createDefault();
				 CloseableHttpResponse response = client.execute(request)) {
				int statusCode = response.getStatusLine().getStatusCode();
				String responseBody = EntityUtils.toString(response.getEntity());
				User leader = userRepository.findById(config.getUserId()).orElse(null);

				if (statusCode == 200 && leader != null) {
					String resolvedName = igName != null ? igName : leader.getName();
					String resolvedUsername = igUsername;
					String resolvedAvatar = igAvatar != null ? igAvatar : leader.getProfilePictureUrl();
					int newPosts = processInstagramResponse(responseBody, config.getOrganizationId(), leader, SOURCE_LEADER_PROFILE,
							resolvedName, resolvedUsername, resolvedAvatar, token, trimTrailingSlash(config.getApiUrl()));
					organizationPlatformSettingsService.markLeaderSyncSuccess(config.getUserId(), Platform.INSTAGRAM, newPosts);
				} else if (leader == null) {
					organizationPlatformSettingsService.markLeaderSyncFailure(config.getUserId(), Platform.INSTAGRAM, "Leader not found");
				} else if (statusCode != 200) {
					log.error("instagram-leader|userId={}|orgId={}|HTTP {}, {}", config.getUserId(), config.getOrganizationId(), statusCode, responseBody);
					if (isMetaDataAccessExpired(statusCode, responseBody)) {
						markLeaderTokenDisconnected(config.getUserId(), Platform.INSTAGRAM, DATA_ACCESS_EXPIRED_REASON);
						organizationPlatformSettingsService.markLeaderSyncFailure(config.getUserId(), Platform.INSTAGRAM, DATA_ACCESS_EXPIRED_REASON);
					}
					else {
						organizationPlatformSettingsService.markLeaderSyncFailure(config.getUserId(), Platform.INSTAGRAM, syncFailureReason(statusCode));
					}
				}
			} catch (Exception e) {
				log.error("instagram leader fetch failed for userId={} orgId={}", config.getUserId(), config.getOrganizationId(), e);
				organizationPlatformSettingsService.markLeaderSyncFailure(config.getUserId(), Platform.INSTAGRAM, syncFailureReason(e));
			}
		}
	}

	@Override
	public void fetchFromExternalPlatformInstagramForOrganization(Long organizationId) {
		if (organizationId == null) {
			return;
		}

		findEnabledOrgConfig(Platform.INSTAGRAM, organizationId)
				.ifPresent(this::fetchInstagramBusinessPageConfig);
	}

	private void fetchInstagramBusinessPageConfig(
			com.social.ripple.external_ingestion.service.OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig config
	) {
		String pageId = config.resolvedExternalId();
		String token = config.getAccessToken();
		String url = String.format("%s/v24.0/%s/media?fields=id,caption,media_type,media_url,timestamp,permalink&access_token=%s",
				trimTrailingSlash(config.getApiUrl()), pageId, token);
		organizationPlatformSettingsService.markSyncStarted(config.getOrganizationId(), Platform.INSTAGRAM);

		// Fetch IG account avatar
		String igAvatar = null;
		try {
			String igInfoUrl = String.format("%s/v24.0/%s?fields=profile_picture_url&access_token=%s",
					trimTrailingSlash(config.getApiUrl()), pageId, token);
			HttpGet igInfoReq = new HttpGet(igInfoUrl);
			igInfoReq.setHeader("Accept", "application/json");
			try (CloseableHttpClient igClient = HttpClients.createDefault();
				 CloseableHttpResponse igResp = igClient.execute(igInfoReq)) {
				if (igResp.getStatusLine().getStatusCode() == 200) {
					JsonNode igInfo = new ObjectMapper().readTree(EntityUtils.toString(igResp.getEntity()));
					igAvatar = igInfo.path("profile_picture_url").asText(null);
				}
			}
		} catch (Exception e) {
			log.warn("instagram|orgId={}|Failed to fetch IG account picture: {}", config.getOrganizationId(), e.getMessage());
		}

		HttpGet request = new HttpGet(url);
		request.setHeader("Accept", "application/json");

		try (CloseableHttpClient client = HttpClients.createDefault();
			 CloseableHttpResponse response = client.execute(request)) {

			int statusCode = response.getStatusLine().getStatusCode();
			String responseBody = EntityUtils.toString(response.getEntity());

			log.info("instagram|orgId={}|responseBody={}", config.getOrganizationId(), responseBody);

			if (statusCode == 200) {
				int newPosts = processInstagramResponse(responseBody, config.getOrganizationId(), getOrganizationPostedUser(config.getOrganizationId()), SOURCE_BUSINESS_PAGE,
						config.getDisplayName(), config.getUsername(), igAvatar, token, trimTrailingSlash(config.getApiUrl()));
				organizationPlatformSettingsService.markSyncSuccess(config.getOrganizationId(), Platform.INSTAGRAM, newPosts);
			} else {
				log.error("instagram|orgId={}|HTTP {}, {}", config.getOrganizationId(), statusCode, responseBody);
				String failureReason = isMetaDataAccessExpired(statusCode, responseBody)
						? DATA_ACCESS_EXPIRED_REASON
						: syncFailureReason(statusCode);
				organizationPlatformSettingsService.markSyncFailure(config.getOrganizationId(), Platform.INSTAGRAM, failureReason);
			}
		} catch (Exception e) {
			log.error("instagram fetch failed for orgId={}", config.getOrganizationId(), e);
			organizationPlatformSettingsService.markSyncFailure(config.getOrganizationId(), Platform.INSTAGRAM, syncFailureReason(e));
		}
	}

	private Optional<com.social.ripple.external_ingestion.service.OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig> findEnabledOrgConfig(
			Platform platform,
			Long organizationId
	) {
		return organizationPlatformSettingsService.getEnabledPlatformConfigs(platform).stream()
				.filter(config -> Objects.equals(config.getOrganizationId(), organizationId))
				.findFirst();
	}

	private int processInstagramResponse(String responseBody, Long organizationId, User ownerUser, String sourceType,
									   String accessToken, String apiBaseUrl) throws Exception {
		return processInstagramResponse(responseBody, organizationId, ownerUser, sourceType, null, null, null, accessToken, apiBaseUrl);
	}

	private int processInstagramResponse(String responseBody, Long organizationId, User ownerUser, String sourceType,
										 String sourceName, String sourceUsername, String sourceAvatarUrl,
										 String accessToken, String apiBaseUrl) throws Exception{
		JsonNode response = mapper.readTree(responseBody);
		int newPosts = 0;

		if (response != null && response.has("data")) {
			JsonNode elements = response.get("data");

			Organization organization = organizationRepository.findById(organizationId).orElseThrow();

			Set<String> existingPostIds = ownerUser == null
					? Set.of()
					: postRepository.findPlatformUniqueIdsByPlatformAndOrganizationAndCreatedBy("INSTAGRAM", organizationId, ownerUser.getId());

			List<Post> posts = new ArrayList<>();

			if (elements != null && elements.isArray()) {
				for (JsonNode postNode : elements) {
					if (!existingPostIds.contains(postNode.get("id").asText())) {
						Post post = parseInstagramPostNode(postNode, organization, ownerUser, sourceType, sourceName, sourceUsername, sourceAvatarUrl);
						postRepository.saveAndFlush(post);
						newPosts++;
						// If have media
						String mediaType = postNode.path("media_type").asText("");
						if (postNode.has("media_url") && ("VIDEO".equals(mediaType) || "IMAGE".equals(mediaType))) {
							PostMedia postMedia = new PostMedia();
							boolean stored = applyFetchedMedia(postMedia, postNode.get("media_url").asText(), mediaType, organization.getId());
							if (stored) {
								postMedia.setPostId(post.getId());
								postMedia.setMediaType(mediaType);
								postMediaRepository.save(postMedia);
								log.info("{} saved", mediaType);
							}
						} else if ("CAROUSEL_ALBUM".equals(mediaType) && accessToken != null) {
							// Fetch all child media for carousel albums
							try {
								String childrenUrl = String.format("%s/v24.0/%s/children?fields=id,media_type,media_url&access_token=%s",
										trimTrailingSlash(apiBaseUrl), postNode.get("id").asText(), accessToken);
								HttpGet childReq = new HttpGet(childrenUrl);
								childReq.setHeader("Accept", "application/json");
								try (CloseableHttpClient childClient = HttpClients.createDefault();
									 CloseableHttpResponse childResp = childClient.execute(childReq)) {
									if (childResp.getStatusLine().getStatusCode() == 200) {
										JsonNode childData = mapper.readTree(EntityUtils.toString(childResp.getEntity())).path("data");
										if (childData.isArray()) {
											for (JsonNode child : childData) {
												String childMediaType = child.path("media_type").asText("");
												if (child.has("media_url") && ("IMAGE".equals(childMediaType) || "VIDEO".equals(childMediaType))) {
													PostMedia postMedia = new PostMedia();
													boolean stored = applyFetchedMedia(postMedia, child.get("media_url").asText(), childMediaType, organization.getId());
													if (stored) {
														postMedia.setPostId(post.getId());
														postMedia.setMediaType(childMediaType);
														postMediaRepository.save(postMedia);
														log.info("CAROUSEL child {} saved", childMediaType);
													}
												}
											}
										}
									}
								}
							} catch (Exception e) {
								log.warn("Failed to fetch carousel children for post {}: {}", postNode.get("id").asText(), e.getMessage());
							}
						}
						posts.add(post);
					}
				}
			}
		}
		return newPosts;
	}

	private boolean applyFetchedMedia(PostMedia postMedia, String externalUrl, String mediaType, Long organizationId) {
		try {
			FetchedMediaStorageService.StoredFetchedMedia storedMedia = fetchedMediaStorageService
					.storeExternalMedia("fb-fetch", externalUrl, mediaType, organizationId);
			postMedia.setFileUrl(storedMedia.getFileUrl());
			postMedia.setPlaybackUrl(storedMedia.getPlaybackUrl());
			postMedia.setThumbnailUrl(storedMedia.getThumbnailUrl());
			postMedia.setStorageProvider(storedMedia.getStorageProvider());
			postMedia.setProcessingStatus(storedMedia.getProcessingStatus());
			postMedia.setMediaType(storedMedia.getMediaType());
			return true;
		} catch (Exception e) {
			log.warn("Failed to store fetched {} in Bunny (skipping): {} | error: {}", mediaType, externalUrl, e.getMessage());
			return false;
		}
	}

	private String trimTrailingSlash(String url) {
		if (url == null) {
			return "";
		}
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	/**
	 * Detects whether a Meta Graph API error response indicates the token's 90-day Data Use Compliance window has expired. When this happens
	 * Meta keeps the token marked {@code is_valid:true} but rejects all data-bearing calls for the user with error code 100 / subcode 33 (or
	 * code 190 / subcode 463 "Session has expired"). The fix is not a token refresh — the user must actively re-authenticate via the OAuth
	 * flow. See {@code data_access_expires_at} on the Meta debug_token response.
	 */
	private boolean isMetaDataAccessExpired(int statusCode, String responseBody) {
		if (statusCode < 400 || responseBody == null || responseBody.isBlank()) {
			return false;
		}
		try {
			JsonNode root = mapper.readTree(responseBody);
			JsonNode error = root.path("error");
			if (error.isMissingNode() || error.isNull()) {
				return false;
			}
			int code = error.path("code").asInt(-1);
			int subcode = error.path("error_subcode").asInt(-1);
			String message = error.path("message").asText("").toLowerCase(Locale.ROOT);
			if (code == 100
					&& (subcode == 33
						|| message.contains("missing permissions")
						|| message.contains("does not exist, cannot be loaded"))) {
				return true;
			}
			if (code == 190
					&& (subcode == 463
						|| message.contains("session has expired")
						|| message.contains("data access"))) {
				return true;
			}
			return false;
		}
		catch (Exception e) {
			return false;
		}
	}

	/**
	 * Marks the leader's user_auth_token row as DISCONNECTED so the dashboard surfaces a clear "reconnect required" state instead of silently
	 * failing every cron run. Idempotent and best-effort: if the row cannot be found or saved, the failure is logged but does not propagate.
	 */
	private void markLeaderTokenDisconnected(Long userId, Platform platform, String reason) {
		if (userId == null) {
			return;
		}
		try {
			userAuthTokenRepository.findByUserIdAndPlatform(userId, platform).ifPresent(token -> {
				token.setStatus(AuthStatus.DISCONNECTED);
				token.setIsConnected(false);
				token.setLastSyncError(reason);
				token.setLastSyncErrorAt(LocalDateTime.now());
				token.setLastSyncStatus("FAILED");
				token.setUpdatedAt(LocalDateTime.now());
				userAuthTokenRepository.save(token);
				log.info("user_auth_token|userId={}|platform={}|Marked DISCONNECTED: {}", userId, platform, reason);
			});
		}
		catch (Exception e) {
			log.warn("Failed to mark user_auth_token disconnected for userId={} platform={}: {}", userId, platform, e.getMessage());
		}
	}

	private String syncFailureReason(int statusCode) {
		return switch (statusCode) {
			case 401 -> "Unauthorized (401)";
			case 404 -> "Not found (404)";
			case 429 -> "Rate limited (429)";
			default -> "HTTP " + statusCode;
		};
	}

	private String syncFailureReason(Exception exception) {
		if (exception == null) {
			return "Unexpected fetch error";
		}
		String message = exception.getMessage();
		if (message == null || message.isBlank()) {
			return exception.getClass().getSimpleName();
		}
		return exception.getClass().getSimpleName() + ": " + message;
	}
}
