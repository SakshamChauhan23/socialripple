package com.social.ripple.external_ingestion.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.ripple.external_ingestion.dao.model.*;
import com.social.ripple.external_ingestion.dao.repository.OrganizationRepository;
import com.social.ripple.external_ingestion.dao.repository.PostMediaRepository;
import com.social.ripple.external_ingestion.dao.repository.PostRepository;
import com.social.ripple.external_ingestion.dao.repository.UserRepository;
import com.social.ripple.external_ingestion.service.IContentFetcherService;
import com.social.ripple.external_ingestion.service.OrganizationPlatformSettingsService;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentFetcherServiceImpl implements IContentFetcherService {

	private static final ObjectMapper mapper = new ObjectMapper();
	private static final String SOURCE_BUSINESS_PAGE = "BUSINESS_PAGE";
	private static final String SOURCE_LEADER_PROFILE = "LEADER_PROFILE";
	private static final int X_MAX_RESULTS = 50;

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
	private OrganizationPlatformSettingsService organizationPlatformSettingsService;

	@Value("${twitter.consumer.key}")
	private String consumerKey;

	@Value("${twitter.consumer.secret}")
	private String consumerSecret;

	@Override
	public void fetchFromExternalPlatformTwitter(Platform platform) {
		log.info("Start fetching");

		List<OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig> configs =
				organizationPlatformSettingsService.getEnabledPlatformConfigs(Platform.X);
		for (OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig config : configs) {
			fetchTwitterBusinessPageConfig(config);
		}

		for (OrganizationPlatformSettingsService.ResolvedLeaderPlatformConfig config :
				organizationPlatformSettingsService.getConnectedLeaderPlatformConfigs(Platform.X)) {
			organizationPlatformSettingsService.markLeaderSyncStarted(config.getUserId(), Platform.X);
			try {
				User leader = userRepository.findById(config.getUserId()).orElse(null);
				if (leader == null) {
					organizationPlatformSettingsService.markLeaderSyncFailure(config.getUserId(), Platform.X, "Leader not found");
					continue;
				}
				XFetchStats stats = fetchXTimelineWindow(
						config.getExternalUserId(),
						config.getAccessToken(),
						config.getAccessSecret(),
						trimTrailingSlash(config.getApiUrl()),
						config.getOrganizationId(),
						leader,
						SOURCE_LEADER_PROFILE,
						leader.getName(),
						null,
						leader.getProfilePictureUrl()
				);
				log.info("x-leader|userId={}|orgId={}|Sync completed|pages={}|newPosts={}",
						config.getUserId(), config.getOrganizationId(), stats.pagesFetched(), stats.newPostsSaved());
				organizationPlatformSettingsService.markLeaderSyncSuccess(config.getUserId(), Platform.X, stats.newPostsSaved());
			} catch (XFetchException e) {
				log.error("x leader fetch failed for userId={} orgId={}|{}", config.getUserId(), config.getOrganizationId(), e.getLogMessage(), e);
				organizationPlatformSettingsService.markLeaderSyncFailure(config.getUserId(), Platform.X, e.getFailureReason());
			} catch (Exception e) {
				log.error("x leader fetch failed for userId={} orgId={}", config.getUserId(), config.getOrganizationId(), e);
				organizationPlatformSettingsService.markLeaderSyncFailure(config.getUserId(), Platform.X, syncFailureReason(e));
			}
		}
	}

	@Override
	public void fetchFromExternalPlatformTwitterForOrganization(Long organizationId) {
		if (organizationId == null) {
			return;
		}

		findEnabledOrgConfig(Platform.X, organizationId)
				.ifPresent(this::fetchTwitterBusinessPageConfig);
	}

	private void fetchTwitterBusinessPageConfig(OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig config) {
		organizationPlatformSettingsService.markSyncStarted(config.getOrganizationId(), Platform.X);
		try {
			XFetchStats stats = fetchXTimelineWindow(
					config.resolvedExternalId(),
					config.getAccessToken(),
					config.getRefreshToken(),
					trimTrailingSlash(config.getApiUrl()),
					config.getOrganizationId(),
					getOrganizationPostedUser(config.getOrganizationId()),
					SOURCE_BUSINESS_PAGE,
					config.getDisplayName(),
					config.getUsername(),
					null
			);
			log.info("x|orgId={}|Sync completed|pages={}|newPosts={}", config.getOrganizationId(), stats.pagesFetched(), stats.newPostsSaved());
			organizationPlatformSettingsService.markSyncSuccess(config.getOrganizationId(), Platform.X, stats.newPostsSaved());
		} catch (XFetchException e) {
			log.error("x|orgId={}|{}", config.getOrganizationId(), e.getLogMessage(), e);
			organizationPlatformSettingsService.markSyncFailure(config.getOrganizationId(), Platform.X, e.getFailureReason());
		} catch (Exception e) {
			log.error("x fetch failed for orgId={}", config.getOrganizationId(), e);
			organizationPlatformSettingsService.markSyncFailure(config.getOrganizationId(), Platform.X, syncFailureReason(e));
		}
	}

	private XFetchStats fetchXTimelineWindow(String userId,
											 String accessToken,
											 String accessSecret,
											 String apiBaseUrl,
											 Long organizationId,
											 User ownerUser,
											 String sourceType) throws Exception {
		return fetchXTimelineWindow(userId, accessToken, accessSecret, apiBaseUrl, organizationId, ownerUser, sourceType, null, null, null);
	}

	private XFetchStats fetchXTimelineWindow(String userId,
											 String accessToken,
											 String accessSecret,
											 String apiBaseUrl,
											 Long organizationId,
											 User ownerUser,
											 String sourceType,
											 String sourceName,
											 String sourceUsername,
											 String sourceAvatarUrl) throws Exception {
		if (userId == null || userId.isBlank()) {
			throw new XFetchException("User not found - Invalid user ID", "X pagination failed");
		}

		Set<String> existingPostIds = ownerUser == null
				? Set.of()
				: new HashSet<>(postRepository.findPlatformUniqueIdsByPlatformAndOrganizationAndCreatedBy("X", organizationId, ownerUser.getId()));

		int pagesFetched = 0;
		int newPostsSaved = 0;
		String nextToken = null;

		try (CloseableHttpClient client = HttpClients.createDefault()) {
			do {
				String url = buildTwitterUrl(apiBaseUrl, userId, X_MAX_RESULTS, nextToken);
				HttpGet request = new HttpGet(url);
				request.setHeader("Content-Type", "application/json");
				request.setHeader("User-Agent", "TwitterJavaClient/1.0");
				applyXAuthorization(request, accessToken, accessSecret);

				try (CloseableHttpResponse response = client.execute(request)) {
					int statusCode = response.getStatusLine().getStatusCode();
					String responseBody = EntityUtils.toString(response.getEntity());
					if (statusCode != 200) {
						throw new XFetchException(buildXLogMessage(organizationId, ownerUser, userId, statusCode, responseBody),
								syncFailureReason(statusCode));
					}

					JsonNode root = mapper.readTree(responseBody);
					pagesFetched++;
					newPostsSaved += processTweetResponse(root, accessToken, organizationId, apiBaseUrl, ownerUser, sourceType, existingPostIds, sourceName, sourceUsername, sourceAvatarUrl);
					nextToken = textValue(root.path("meta"), "next_token");
				}
			} while (hasText(nextToken));
		}

		return new XFetchStats(pagesFetched, newPostsSaved);
	}

	private void applyXAuthorization(HttpGet request, String accessToken, String accessSecret) {
		try {
			if (accessSecret != null && !accessSecret.isBlank()) {
				OAuthConsumer consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
				consumer.setTokenWithSecret(accessToken, accessSecret);
				consumer.sign(request);
				return;
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to sign X request", e);
		}

		request.setHeader("Authorization", "Bearer " + accessToken);
	}

	@Override
	public void fetchFromExternalPlatformLinkedIn(Platform platform) {
		log.info("Start fetching Linked In");
		List<OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig> configs =
				organizationPlatformSettingsService.getEnabledPlatformConfigs(Platform.LINKEDIN);
		for (OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig config : configs) {
			fetchLinkedInBusinessPageConfig(config);
		}

		for (OrganizationPlatformSettingsService.ResolvedLeaderPlatformConfig config :
				organizationPlatformSettingsService.getConnectedLeaderPlatformConfigs(Platform.LINKEDIN)) {
			organizationPlatformSettingsService.markLeaderSyncStarted(config.getUserId(), Platform.LINKEDIN);
			try {
				int newPosts = fetchLeaderLinkedInPosts(config);
				log.info("linkedin-leader|userId={}|orgId={}|Sync completed|newPosts={}",
						config.getUserId(), config.getOrganizationId(), newPosts);
				organizationPlatformSettingsService.markLeaderSyncSuccess(config.getUserId(), Platform.LINKEDIN, newPosts);
			} catch (Exception e) {
				log.error("linkedin leader fetch failed for userId={} orgId={}", config.getUserId(), config.getOrganizationId(), e);
				organizationPlatformSettingsService.markLeaderSyncFailure(config.getUserId(), Platform.LINKEDIN, syncFailureReason(e));
			}
		}
	}

	@Override
	public void fetchFromExternalPlatformLinkedInForOrganization(Long organizationId) {
		if (organizationId == null) {
			return;
		}

		findEnabledOrgConfig(Platform.LINKEDIN, organizationId)
				.ifPresent(this::fetchLinkedInBusinessPageConfig);
	}

	private void fetchLinkedInBusinessPageConfig(OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig config) {
		String userId = config.resolvedExternalId();
		String tokenOld = config.getAccessToken();
		organizationPlatformSettingsService.markSyncStarted(config.getOrganizationId(), Platform.LINKEDIN);

		String url = buildLinkedInUrl(trimTrailingSlash(config.getApiUrl()), userId, 50);

		HttpGet request = new HttpGet(url);
		request.setHeader("Authorization", "Bearer " + tokenOld);
		request.setHeader("Content-Type", "application/json");

		try (CloseableHttpClient client = HttpClients.createDefault();
			 CloseableHttpResponse response = client.execute(request)) {

			int statusCode = response.getStatusLine().getStatusCode();
			String responseBody = EntityUtils.toString(response.getEntity());

			if (statusCode == 200) {
				int newPosts = processLinkedInResponse(responseBody, config.getOrganizationId(), tokenOld);
				organizationPlatformSettingsService.markSyncSuccess(config.getOrganizationId(), Platform.LINKEDIN, newPosts);
			} else if (statusCode == 401) {
				log.error("linkedin|orgId={}|Unauthorized - Check your Bearer Token", config.getOrganizationId());
				organizationPlatformSettingsService.markSyncFailure(config.getOrganizationId(), Platform.LINKEDIN, syncFailureReason(statusCode));
			} else if (statusCode == 429) {
				log.error("linkedin|orgId={}|Rate limit exceeded - Too many requests", config.getOrganizationId());
				organizationPlatformSettingsService.markSyncFailure(config.getOrganizationId(), Platform.LINKEDIN, syncFailureReason(statusCode));
			} else if (statusCode == 404) {
				log.error("linkedin|orgId={}|User not found - Invalid user ID: {}", config.getOrganizationId(), userId);
				organizationPlatformSettingsService.markSyncFailure(config.getOrganizationId(), Platform.LINKEDIN, syncFailureReason(statusCode));
			} else {
				log.error("linkedin|orgId={}|HTTP {}, {}", config.getOrganizationId(), statusCode, responseBody);
				organizationPlatformSettingsService.markSyncFailure(config.getOrganizationId(), Platform.LINKEDIN, syncFailureReason(statusCode));
			}
		} catch (Exception e) {
			log.error("linkedin fetch failed for orgId={}", config.getOrganizationId(), e);
			organizationPlatformSettingsService.markSyncFailure(config.getOrganizationId(), Platform.LINKEDIN, syncFailureReason(e));
		}
	}

	private Optional<OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig> findEnabledOrgConfig(Platform platform, Long organizationId) {
		return organizationPlatformSettingsService.getEnabledPlatformConfigs(platform).stream()
				.filter(config -> Objects.equals(config.getOrganizationId(), organizationId))
				.findFirst();
	}

	private String getFormattedDate(Date date) {
		SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		isoFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Important: Twitter requires UTC
		return isoFormat.format(date);
	}

	private static String buildTwitterUrl(String apiBaseUrl, String userId, int maxResults, String nextToken) {

		String baseUrl = String.format(
				"%s/2/users/%s/tweets?" +
						"max_results=%d&" +
						"tweet.fields=created_at,author_id,public_metrics,source,attachments,entities,referenced_tweets&" +
						"expansions=attachments.media_keys,author_id&" +
						"media.fields=url,preview_image_url,type,height,width,duration_ms,alt_text&" +
						"user.fields=name,username,profile_image_url&" +
						"exclude=retweets,replies",
				apiBaseUrl, userId, maxResults
		);
		if (nextToken == null || nextToken.isBlank()) {
			return baseUrl;
		}
		return baseUrl + "&pagination_token=" + nextToken;
	}

	private static String buildLinkedInUrl(String apiBaseUrl, String userId, int maxResults) {

		return String.format(
				"%s/v2/shares?q=owners&owners=urn:li:organization:%s&count=%d",
				apiBaseUrl, userId, maxResults
		);
	}

	private int processTweetResponse(JsonNode root, String token, Long organizationId, String apiBaseUrl,
									  User ownerUser, String sourceType, Set<String> existingPostIds) throws Exception {
		return processTweetResponse(root, token, organizationId, apiBaseUrl, ownerUser, sourceType, existingPostIds, null, null, null);
	}

	private int processTweetResponse(JsonNode root, String token, Long organizationId, String apiBaseUrl,
									  User ownerUser, String sourceType, Set<String> existingPostIds,
									  String sourceName, String sourceUsername, String sourceAvatarUrl) throws Exception {
		log.info("root:{}", root);
		JsonNode data = root.get("data");

		Organization organization = organizationRepository.findById(organizationId).orElseThrow();
		int savedCount = 0;

		JsonNode includesNode = root.path("includes");
		JsonNode mediaNode = includesNode.path("media");

		// Create media map for quick lookup
		Map<String, JsonNode> mediaMap = new HashMap<>();
		if (mediaNode.isArray()) {
			for (JsonNode media : mediaNode) {
				String mediaKey = media.path("media_key").asText();
				mediaMap.put(mediaKey, media);
			}
		}

		// Create user map for author profile info
		JsonNode usersNode = includesNode.path("users");
		Map<String, JsonNode> userMap = new HashMap<>();
		if (usersNode.isArray()) {
			for (JsonNode user : usersNode) {
				userMap.put(user.path("id").asText(), user);
			}
		}

		if (data != null && data.isArray()) {
			for (JsonNode tweetNode : data) {
				String tweetId = tweetNode.get("id").asText();
				if (!existingPostIds.contains(tweetId)) {
					// Resolve author profile from X API response (overrides fallback user name)
					String resolvedName = sourceName;
					String resolvedUsername = sourceUsername;
					String resolvedAvatar = sourceAvatarUrl;
					String authorId = textValue(tweetNode, "author_id");
					if (hasText(authorId)) {
						JsonNode authorNode = userMap.get(authorId);
						if (authorNode != null) {
							String apiName = textValue(authorNode, "name");
							String apiUsername = textValue(authorNode, "username");
							String apiAvatar = textValue(authorNode, "profile_image_url");
							if (hasText(apiName)) resolvedName = apiName;
							if (hasText(apiUsername)) resolvedUsername = "@" + apiUsername;
							if (hasText(apiAvatar)) resolvedAvatar = apiAvatar;
						}
					}
					Post post = parseTweetNode(tweetNode, organization, ownerUser, sourceType, resolvedName, resolvedUsername, resolvedAvatar);
					postRepository.saveAndFlush(post);
					existingPostIds.add(tweetId);
					savedCount++;

					JsonNode attachmentsNode = tweetNode.path("attachments");
					if (!attachmentsNode.isMissingNode()) {
						JsonNode mediaKeysNode = attachmentsNode.path("media_keys");
						if (mediaKeysNode.isArray()) {
							for (JsonNode mediaKeyNode : mediaKeysNode) {
								String mediaKey = mediaKeyNode.asText();
								JsonNode mediaInfo = mediaMap.get(mediaKey);
								if (mediaInfo != null) {
									PostMedia media = new PostMedia();
									boolean isVideo = "video".equals(mediaInfo.path("type").asText());
									String sourceUrl = isVideo ? getVideoUrlX(tweetNode.get("id").asText(), token, apiBaseUrl) : mediaInfo.path("url").asText();
									String mediaType = isVideo ? "VIDEO" : "IMAGE";
									if (!hasText(sourceUrl) && isVideo) {
										sourceUrl = mediaInfo.path("preview_image_url").asText();
										mediaType = "IMAGE";
									}
									applyFetchedMedia(media, sourceUrl, mediaType, organization.getId(), "x-fetch");

									media.setPostId(post.getId());
									postMediaRepository.save(media);
								}
							}
						}
					}
				}
			}
		}
		return savedCount;
	}

	private String buildXLogMessage(Long organizationId, User ownerUser, String userId, int statusCode, String responseBody) {
		if (ownerUser != null) {
			return String.format("x-leader|userId=%s|orgId=%s|HTTP %s, %s", ownerUser.getId(), organizationId, statusCode, responseBody);
		}
		if (statusCode == 401) {
			return String.format("x|orgId=%s|Unauthorized - Check your Bearer Token", organizationId);
		}
		if (statusCode == 429) {
			return String.format("x|orgId=%s|Rate limit exceeded - Too many requests", organizationId);
		}
		if (statusCode == 404) {
			return String.format("x|orgId=%s|User not found - Invalid user ID: %s", organizationId, userId);
		}
		return String.format("x|orgId=%s|HTTP %s, %s", organizationId, statusCode, responseBody);
	}

	private String textValue(JsonNode node, String fieldName) {
		if (node == null || node.isMissingNode()) {
			return null;
		}
		JsonNode field = node.path(fieldName);
		if (field.isMissingNode() || field.isNull()) {
			return null;
		}
		String value = field.asText();
		return hasText(value) ? value : null;
	}

	private Post parseTweetNode(JsonNode tweetNode, Organization organization, User ownerUser, String sourceType) {
		return parseTweetNode(tweetNode, organization, ownerUser, sourceType, null, null, null);
	}

	private Post parseTweetNode(JsonNode tweetNode, Organization organization, User ownerUser, String sourceType,
								String sourceName, String sourceUsername, String sourceAvatarUrl) {
		Post post = new Post();

		post.setType("X");
		post.setSourceType(sourceType);
		post.setPlatformUniqueId(tweetNode.get("id").asText());
		post.setSourceName(sourceName);
		post.setSourceUsername(sourceUsername);
		post.setSourceAvatarUrl(sourceAvatarUrl);
		post.setContent(cleanTweetText(tweetNode.get("text").asText()));
		post.setStatus("PUBLISHED");

		post.setOrganization(organization);

		if (tweetNode.has("created_at")) {
			try {
				String createdAtStr = tweetNode.get("created_at").asText();
				Instant instant = Instant.parse(createdAtStr);
				post.setPlatformCreatedAt(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
				post.setCreatedAt(LocalDateTime.now());
			} catch (Exception e) {
				post.setPlatformCreatedAt(LocalDateTime.now());
				post.setCreatedAt(LocalDateTime.now());
			}
		} else {
			post.setPlatformCreatedAt(LocalDateTime.now());
			post.setCreatedAt(LocalDateTime.now());
		}

		if (ownerUser != null) {
			post.setCreatedBy(ownerUser);
		}

		return post;
	}

	private Post parseLinkedInPostNode(JsonNode postNode, Organization organization, User ownerUser, String sourceType) {
		return parseLinkedInPostNode(postNode, organization, ownerUser, sourceType, null, null, null);
	}

	private Post parseLinkedInPostNode(JsonNode postNode, Organization organization, User ownerUser, String sourceType,
									   String sourceName, String sourceUsername, String sourceAvatarUrl) {
		Post post = new Post();

		post.setType("LINKEDIN");
		post.setSourceType(sourceType);
		post.setPlatformUniqueId(postNode.get("id").asText());
		post.setSourceName(sourceName);
		post.setSourceUsername(sourceUsername);
		post.setSourceAvatarUrl(sourceAvatarUrl);

		if (postNode.has("text") && postNode.get("text").has("text")) {
			post.setContent(cleanUnicode(postNode.get("text").get("text").asText()));
		}
		post.setStatus("PUBLISHED");

		post.setOrganization(organization);

		if (postNode.has("created")) {
			try {
				long epochSeconds = postNode.get("created").get("time").asLong();
				LocalDateTime createdDateTime = LocalDateTime.ofInstant(
						Instant.ofEpochSecond(epochSeconds / 1000),
						ZoneId.systemDefault()
				);
				post.setPlatformCreatedAt(createdDateTime);
				post.setCreatedAt(LocalDateTime.now());
			} catch (Exception e) {
				post.setPlatformCreatedAt(LocalDateTime.now());
				post.setCreatedAt(LocalDateTime.now());
			}
		} else {
			post.setPlatformCreatedAt(LocalDateTime.now());
			post.setCreatedAt(LocalDateTime.now());
		}

		if (ownerUser != null) {
			post.setCreatedBy(ownerUser);
		}


		return post;
	}

	private User getOrganizationPostedUser(Long id) {
		return organizationPlatformSettingsService.findOrganizationPostedUser(id).orElse(null);
	}

	private String cleanTweetText(String text) {
		if (text == null) return null;

		return text
				.replaceAll("https://t\\.co/\\w+", "")
				.replaceAll("pic\\.twitter\\.com/\\w+", "")
				.replaceAll("\\s+", " ")
				.replaceAll("^\\s*[,\\.]\\s*", "")
				.replaceAll("\\s*[,\\.]\\s*$", "")
				.trim();
	}

	private int processLinkedInResponse(String responseBody, Long organizationId, String linkedinAccessToken) throws Exception {
		JsonNode root = mapper.readTree(responseBody);
		log.info("root:{}", root);
		int newPosts = 0;

		if (root.has("elements")) {
			JsonNode elements = root.get("elements");

			Organization organization = organizationRepository.findById(organizationId).orElseThrow();

			Set<String> existingPostIds = postRepository.findPlatformUniqueIdsByPlatformAndOrganization("LINKEDIN", organizationId);

			List<Post> posts = new ArrayList<>();

			if (elements != null && elements.isArray()) {
				for (JsonNode tweetNode : elements) {
					if (!existingPostIds.contains(tweetNode.get("id").asText())) {
						Post post = parseLinkedInPostNode(
								tweetNode,
								organization,
								getOrganizationPostedUser(organizationId),
								SOURCE_BUSINESS_PAGE
						);
						postRepository.saveAndFlush(post);
						newPosts++;
						// If have media
						if (tweetNode.has("content")) {
							JsonNode contentNode = tweetNode.get("content");
							if (contentNode.has("contentEntities") && contentNode.get("contentEntities").isArray()) {
								JsonNode entitiesArray = contentNode.get("contentEntities");

								for (JsonNode entityNode : entitiesArray) {
									PostMedia postMedia = parseContentEntity(entityNode, organization, linkedinAccessToken);
									postMedia.setPostId(post.getId());
									postMedia.setMediaType("IMAGE");
									postMediaRepository.save(postMedia);
								}
							}
						}
						posts.add(post);
					}
				}
			}

//			if (!posts.isEmpty()) {
//				postRepository.saveAll(posts);
//			}
		} else {
			log.info("no data");
		}
		return newPosts;
	}

	private PostMedia parseContentEntity(JsonNode entityNode, Organization organization, String linkedinAccessToken) {
		PostMedia entity = new PostMedia();
		applyFetchedMedia(entity, getTextSafe(entityNode, "entityLocation"), "IMAGE", organization.getId(), "linkedin-fetch", linkedinAccessToken);
		return entity;
	}

	private String getTextSafe(JsonNode node, String fieldName) {
		if (node != null && node.has(fieldName) && !node.get(fieldName).isNull()) {
			return node.get(fieldName).asText();
		}
		return null;
	}

	public String getVideoUrlX(String tweetId, String token, String apiBaseUrl) {
		String url = String.format("%s/2/tweets/%s?expansions=attachments.media_keys&media.fields=url,type,preview_image_url,variants",
				apiBaseUrl,
				tweetId);

		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet request = new HttpGet(url);

		// Set headers
		request.setHeader("Authorization", "Bearer " + token);
		request.setHeader("Content-Type", "application/json");

		try (CloseableHttpResponse response = client.execute(request)) {
			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode == 200) {
				String responseBody = EntityUtils.toString(response.getEntity());
				JsonNode root = mapper.readTree(responseBody);
				return extractVideoUrlFromTweet(root);
			} else {
				throw new RuntimeException("Tweet not found: " + tweetId);
			}

		} catch (Exception e) {
			log.error("error:{}", e);
		} finally {
			try {
				client.close();
			} catch (Exception e) {
				// Log closing error but don't throw
				System.err.println("Error closing HttpClient: " + e.getMessage());
			}
		}
		return null;
	}

	private String extractVideoUrlFromTweet(JsonNode root) {
		JsonNode includesNode = root.path("includes");
		JsonNode mediaNode = includesNode.path("media");

		if (mediaNode.isArray()) {
			for (JsonNode media : mediaNode) {
				String type = media.path("type").asText();
				if ("video".equals(type) || "animated_gif".equals(type)) {
					JsonNode variantsNode = media.path("variants");

					if (variantsNode.isArray()) {
						for (JsonNode variantNode : variantsNode) {
							return variantNode.path("url").asText();
						}
					}
				}
			}
		}

		return null;
	}

	@Override
	public void fetchFromExternalPlatformLinkedinVideoOnly(Platform platform) {
		log.info("Start fetching Linked In videos");
		List<OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig> configs =
				organizationPlatformSettingsService.getEnabledPlatformConfigs(Platform.LINKEDIN);

		for (OrganizationPlatformSettingsService.ResolvedOrgPlatformConfig config : configs) {
			String userId = config.resolvedExternalId();
			String token = config.getAccessToken();

			try (CloseableHttpClient client = HttpClients.createDefault()) {
				String apiUrl = trimTrailingSlash(config.getApiUrl()) + "/rest/posts?q=author&author=urn%3Ali%3Aorganization%3A" + userId + "&count=50";

				JsonNode responseNode = executeGetRequestLinkedinVideo(client, apiUrl, token);
				JsonNode elements = responseNode.path("elements");

				log.info("linkedin-video|orgId={}|Found {} posts to process", config.getOrganizationId(), elements.size());

				if (elements != null && elements.isArray()) {

					Organization organization = organizationRepository.findById(config.getOrganizationId()).orElseThrow();

					Set<String> existingPostIds = postRepository.findPlatformUniqueIdsByPlatformAndOrganization("LINKEDIN", config.getOrganizationId());

					for (JsonNode tweetNode : elements) {
						String inputId = tweetNode.get("id").asText().substring(tweetNode.get("id").asText().contains("li:") ? 15 : 13);
						if (!existingPostIds.contains(inputId) && tweetNode.has("content") &&
								tweetNode.get("content").has("media") &&
								tweetNode.get("content").get("media").has("id") &&
								tweetNode.get("content").get("media").get("id").asText().contains("video")) {
							Post post = parseLinkedInPostNodeVideo(
									tweetNode,
									organization,
									getOrganizationPostedUser(config.getOrganizationId()),
									SOURCE_BUSINESS_PAGE
							);
							postRepository.saveAndFlush(post);
							if (tweetNode.has("content")) {
								JsonNode contentNode = tweetNode.get("content");
								if (contentNode.has("media") && contentNode.get("media").has("id") && contentNode.get("media").get("id").asText().contains("video")) {
									String downloadUrl = getVideoDownloadUrl(contentNode.get("media").get("id").asText(), token, trimTrailingSlash(config.getApiUrl()));
									PostMedia postMedia = new PostMedia();
									applyFetchedMedia(postMedia, downloadUrl, "VIDEO", organization.getId(), "linkedin-video-fetch", token);
									postMedia.setPostId(post.getId());
									postMedia.setMediaType("VIDEO");
									postMediaRepository.save(postMedia);
								}
							}
						}
					}
				}
			} catch (Exception e) {
				log.error("linkedin-video fetch failed for orgId={}", config.getOrganizationId(), e);
			}
		}
	}

	private int fetchLeaderLinkedInPosts(OrganizationPlatformSettingsService.ResolvedLeaderPlatformConfig config) throws Exception {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			if (!hasText(config.getExternalUserId())) {
				throw new IOException("Missing LinkedIn external user id for leader sync");
			}
			String apiUrl = trimTrailingSlash(config.getApiUrl())
					+ "/rest/posts?q=author&author=urn%3Ali%3Aperson%3A"
					+ config.getExternalUserId()
					+ "&count=50";

			JsonNode responseNode;
			try {
				responseNode = executeGetRequestLinkedinVideo(client, apiUrl, config.getAccessToken());
			} catch (IOException primaryError) {
				log.warn("linkedin-leader|userId={}|orgId={}|Primary personal posts fetch failed, falling back to shares API: {}",
						config.getUserId(), config.getOrganizationId(), primaryError.getMessage());
				String fallbackUrl = trimTrailingSlash(config.getApiUrl())
						+ "/v2/shares?q=owners&owners=urn:li:person:"
						+ config.getExternalUserId()
						+ "&count=50";
				responseNode = executeGetRequestLinkedinFallback(client, fallbackUrl, config.getAccessToken());
			}
			JsonNode elements = responseNode.path("elements");
			if (elements == null || !elements.isArray()) {
				return 0;
			}

			Organization organization = organizationRepository.findById(config.getOrganizationId()).orElseThrow();
			User leader = userRepository.findById(config.getUserId()).orElse(null);
			if (leader == null) {
				throw new IllegalStateException("Leader not found");
			}

			Set<String> existingPostIds = postRepository.findPlatformUniqueIdsByPlatformAndOrganizationAndCreatedBy(
					"LINKEDIN",
					config.getOrganizationId(),
					leader.getId()
			);
			int newPosts = 0;

			for (JsonNode postNode : elements) {
				String normalizedId = normalizeLinkedInPostId(postNode.path("id").asText());
				if (existingPostIds.contains(normalizedId)) {
					continue;
				}

				// Use leader name for LinkedIn (API doesn't expose profile pic easily)
			// but set sourceUsername from the external platform ID
			String linkedInSourceName = leader.getName();
			String linkedInSourceUsername = hasText(config.getExternalUserId()) ? config.getExternalUserId() : null;
			Post post = isLinkedInRestPost(postNode)
						? parseLinkedInPostNodeVideo(postNode, organization, leader, SOURCE_LEADER_PROFILE, linkedInSourceName, linkedInSourceUsername, leader.getProfilePictureUrl())
						: parseLinkedInPostNode(postNode, organization, leader, SOURCE_LEADER_PROFILE, linkedInSourceName, linkedInSourceUsername, leader.getProfilePictureUrl());
				post.setPlatformUniqueId(normalizedId);
				postRepository.saveAndFlush(post);
				existingPostIds.add(normalizedId);
				newPosts++;

				if (isLinkedInRestPost(postNode)
						&& postNode.has("content")
						&& postNode.get("content").has("media")
						&& postNode.get("content").get("media").has("id")
						&& postNode.get("content").get("media").get("id").asText().contains("video")) {
					String downloadUrl = getVideoDownloadUrl(
							postNode.get("content").get("media").get("id").asText(),
							config.getAccessToken(),
							trimTrailingSlash(config.getApiUrl())
					);
					PostMedia postMedia = new PostMedia();
					applyFetchedMedia(postMedia, downloadUrl, "VIDEO", organization.getId(), "linkedin-leader-fetch", config.getAccessToken());
					postMedia.setPostId(post.getId());
					postMedia.setMediaType("VIDEO");
					postMediaRepository.save(postMedia);
				}
			}
			return newPosts;
		}
	}

	private JsonNode executeGetRequestLinkedinVideo(CloseableHttpClient client, String url, String token) throws IOException {
		HttpGet request = new HttpGet(url);
		request.setHeader("Authorization", "Bearer " + token);
		request.setHeader("X-Restli-Protocol-Version", "2.0.0");
		request.setHeader("LinkedIn-Version", "202509");

		try (CloseableHttpResponse response = client.execute(request)) {
			HttpEntity entity = response.getEntity();
			String responseBody = EntityUtils.toString(entity);

			log.info("response: {}",responseBody);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new IOException("HTTP " + response.getStatusLine().getStatusCode() + " - " + responseBody);
			}

			return mapper.readTree(responseBody);
		}
	}

	private JsonNode executeGetRequestLinkedinFallback(CloseableHttpClient client, String url, String token) throws IOException {
		HttpGet request = new HttpGet(url);
		request.setHeader("Authorization", "Bearer " + token);
		request.setHeader("Content-Type", "application/json");

		try (CloseableHttpResponse response = client.execute(request)) {
			HttpEntity entity = response.getEntity();
			String responseBody = EntityUtils.toString(entity);
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new IOException("HTTP " + response.getStatusLine().getStatusCode() + " - " + responseBody);
			}
			return mapper.readTree(responseBody);
		}
	}

	private boolean isLinkedInRestPost(JsonNode postNode) {
		return postNode != null && postNode.has("createdAt");
	}

	private String normalizeLinkedInPostId(String rawId) {
		if (rawId == null || rawId.isBlank()) {
			return rawId;
		}
		if (rawId.length() > 15 && rawId.contains("li:")) {
			return rawId.substring(15);
		}
		if (rawId.length() > 13) {
			return rawId.substring(13);
		}
		return rawId;
	}

	private Post parseLinkedInPostNodeVideo(JsonNode postNode, Organization organization, User ownerUser, String sourceType) {
		return parseLinkedInPostNodeVideo(postNode, organization, ownerUser, sourceType, null, null, null);
	}

	private Post parseLinkedInPostNodeVideo(JsonNode postNode, Organization organization, User ownerUser, String sourceType,
											String sourceName, String sourceUsername, String sourceAvatarUrl) {
		Post post = new Post();

		post.setType("LINKEDIN");
		post.setSourceType(sourceType);
		post.setPlatformUniqueId(postNode.get("id").asText().substring(postNode.get("id").asText().contains("li")?15:13));
		post.setSourceName(sourceName);
		post.setSourceUsername(sourceUsername);
		post.setSourceAvatarUrl(sourceAvatarUrl);

		if (postNode.has("commentary")) {
			post.setContent(cleanUnicode(postNode.get("commentary").asText()));
		}
		post.setStatus("PUBLISHED");

		post.setOrganization(organization);

		if (postNode.has("createdAt")) {
			try {
				long epochSeconds = postNode.get("createdAt").asLong();
				LocalDateTime createdDateTime = LocalDateTime.ofInstant(
						Instant.ofEpochSecond(epochSeconds / 1000),
						ZoneId.systemDefault()
				);
				post.setPlatformCreatedAt(createdDateTime);
				post.setCreatedAt(LocalDateTime.now());
			} catch (Exception e) {
				post.setPlatformCreatedAt(LocalDateTime.now());
				post.setCreatedAt(LocalDateTime.now());
			}
		} else {
			post.setPlatformCreatedAt(LocalDateTime.now());
			post.setCreatedAt(LocalDateTime.now());
		}

		if (ownerUser != null) {
			post.setCreatedBy(ownerUser);
		}


		return post;
	}

	private String getVideoDownloadUrl(String videoUrn, String token, String apiBaseUrl) throws IOException {
		String apiUrl = apiBaseUrl + "/rest/videos/" + videoUrn.replace(":", "%3A");

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + token);
		headers.set("X-Restli-Protocol-Version", "2.0.0");
		headers.set("LinkedIn-Version", "202509");
		headers.setContentType(MediaType.APPLICATION_JSON);

		org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

		log.info("before download call");

		// Make the request
		ResponseEntity<String> response = restTemplate.exchange(
				apiUrl,
				HttpMethod.GET,
				entity,
				String.class
		);

		log.info("responseBody: {}", response.getBody());

//		if (response.getStatusCode() == HttpStatus.OK) {
			JsonNode rootNode = mapper.readTree(response.getBody());

			// Extract download URL from response
		if(rootNode.has("downloadUrl")) {
			return rootNode.get("downloadUrl").asText();
		}
//			if (!downloadUrlNode.isMissingNode()) {
//				return downloadUrlNode.path("url").asText();
//			}
//
//			// Alternative: try to get streaming URL
//			JsonNode streamingUrlNode = rootNode.path("streamingUrl");
//			if (!streamingUrlNode.isMissingNode()) {
//				return streamingUrlNode.asText();
//			}
//		}
		return null;
	}

	private String trimTrailingSlash(String url) {
		if (url == null) {
			return "";
		}
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}

	private boolean applyFetchedMedia(PostMedia postMedia, String externalUrl, String mediaType, Long organizationId, String traceId) {
		return applyFetchedMedia(postMedia, externalUrl, mediaType, organizationId, traceId, null);
	}

	private boolean applyFetchedMedia(PostMedia postMedia, String externalUrl, String mediaType, Long organizationId, String traceId,
			String bearerToken) {
		try {
			Map<String, String> downloadHeaders = hasText(bearerToken)
					? Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
					: Collections.emptyMap();
			FetchedMediaStorageService.StoredFetchedMedia storedMedia = fetchedMediaStorageService
					.storeExternalMedia(traceId, externalUrl, mediaType, organizationId, downloadHeaders);
			postMedia.setFileUrl(storedMedia.getFileUrl());
			postMedia.setPlaybackUrl(storedMedia.getPlaybackUrl());
			postMedia.setThumbnailUrl(storedMedia.getThumbnailUrl());
			postMedia.setStorageProvider(storedMedia.getStorageProvider());
			postMedia.setProcessingStatus(storedMedia.getProcessingStatus());
			postMedia.setMediaType(storedMedia.getMediaType());
			return true;
		} catch (Exception e) {
			log.warn("[{}]|Failed to store fetched {} in Bunny (skipping): {} | error: {}", traceId, mediaType, externalUrl, e.getMessage());
			return false;
		}
	}

	private boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	private record XFetchStats(int pagesFetched, int newPostsSaved) {
	}

	private static final class XFetchException extends RuntimeException {
		private final String failureReason;
		private final String logMessage;

		private XFetchException(String logMessage, String failureReason) {
			super(logMessage);
			this.logMessage = logMessage;
			this.failureReason = failureReason;
		}

		private String getFailureReason() {
			return failureReason;
		}

		private String getLogMessage() {
			return logMessage;
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

	public static String cleanUnicode(String input) {
		if (input == null) return null;
		// Remove or replace problematic characters
		return input.replaceAll("[^\\x00-\\x7F]", "");
		// Or keep only basic printable ASCII
	}
}
