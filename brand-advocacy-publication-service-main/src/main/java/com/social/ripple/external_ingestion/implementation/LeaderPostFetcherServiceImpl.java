package com.social.ripple.external_ingestion.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.ripple.external_ingestion.constants.ApplicationConstants;
import com.social.ripple.external_ingestion.dao.model.*;
import com.social.ripple.external_ingestion.dao.repository.*;
import com.social.ripple.external_ingestion.dto.CustomMultipartFile;
import com.social.ripple.external_ingestion.service.ILeaderPostFetcherService;
import com.social.ripple.external_ingestion.util.enumeration.AppCache;
import com.social.ripple.external_ingestion.util.enumeration.ConfigKeys;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaderPostFetcherServiceImpl implements ILeaderPostFetcherService {

	private static final ObjectMapper mapper = new ObjectMapper();

	@Value("${twitter.consumer.key}")
	private String consumerKey;

	@Value("${twitter.consumer.secret}")
	private String consumerSecret;

	@Autowired
	private UserAuthTokenRepository userAuthTokenRepository;

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private OrganizationRepository organizationRepository;

	@Autowired
	private PostMediaRepository postMediaRepository;

	@Autowired
	private UserRepository userRepository;

	@Override
	public void fetchLeadPostFromExternalPlatformTwitter(Platform platform) {
		log.info("Start fetching leader posts from Twitter");
		Calendar calendar = Calendar.getInstance();
		String endTime = getFormattedDate(calendar.getTime());
		calendar.add(Calendar.DAY_OF_YEAR, -1);
		String startTime = getFormattedDate(calendar.getTime());

		// Dev
//		String userId = "1974481760345051136";
//		String token = "AAAAAAAAAAAAAAAAAAAAANxl4gEAAAAABshCPiDqssE0Bb0aYi%2FVPHjYy6c%3DN6Qqmh86K7oU4l7PYosndFvizd0Hzed3uBTefpq0vc9sniOnKE";
		String url = null;

		Optional<UserAuthToken> userAuthTokenData = userAuthTokenRepository.findByUserIdAndPlatform(99L, platform);
		if (userAuthTokenData.isPresent()) {
			UserAuthToken userAuthToken = userAuthTokenData.get();
			String accessToken = userAuthToken.getAccessToken();
			String accessSecret = userAuthToken.getAccessSecret();
			OAuthConsumer consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
			consumer.setTokenWithSecret(accessToken, accessSecret);
			url = buildTwitterUrl(userAuthToken.getUserIdExternal(), startTime, endTime, 50);

			HttpGet request = new HttpGet(url);
//			request.setHeader("Authorization", "Bearer " + token);
			request.setHeader("Content-Type", "application/json");
//			request.setHeader("User-Agent", "TwitterJavaClient/1.0");

            try {
                consumer.sign(request);
            } catch (Exception e) {
                log.error("error in signing request: {}",e.getMessage());
            }

			try (CloseableHttpClient client = HttpClients.createDefault();
				 CloseableHttpResponse response = client.execute(request)) {

				int statusCode = response.getStatusLine().getStatusCode();
				String responseBody = EntityUtils.toString(response.getEntity());

				log.info("responseBody: {}",responseBody);

				return;

//				if (statusCode == 200) {
//					processTweetResponse(responseBody, token, platform);
////				return new TweetResponse(responseBody, parseTweetCount(responseBody));
//				} else if (statusCode == 401) {
//					log.error("Unauthorized - Check your Bearer Token");
//				} else if (statusCode == 429) {
//					log.error("Rate limit exceeded - Too many requests");
//				} else if (statusCode == 404) {
//					log.error("User not found - Invalid user ID: {}", userId);
//				} else {
//					log.error("HTTP {}, {}", statusCode, responseBody);
//				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}


        }

	}

	private String getFormattedDate(Date date) {
		SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		isoFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Important: Twitter requires UTC
		return isoFormat.format(date);
	}

	private static String buildTwitterUrl(String userId, String startTime,
										  String endTime, int maxResults) {

		return "https://api.twitter.com/2/users/1974481760345051136/tweets?max_results=10";

//		return String.format(
//				"https://api.twitter.com/2/users/%s/tweets?" +
//						"max_results=%d&" +
//						"start_time=%s&" +
//						"end_time=%s&" +
//						"tweet.fields=created_at,public_metrics,source,attachments,entities,referenced_tweets&" +
//						"expansions=attachments.media_keys&" +
//						"media.fields=url,preview_image_url,type,height,width,duration_ms,alt_text&" +
//						"exclude=retweets,replies",
//				userId, maxResults, startTime, endTime
//		);
	}

	private static String buildLinkedInUrl(String userId, long startTime,
										   long endTime, int maxResults) {

		return String.format(
				"https://api.linkedin.com/v2/shares?q=owners&owners=urn:li:organization:%s&count=100",
				userId
		);
	}

	private void processTweetResponse(String responseBody, String token, Platform platform) throws Exception {
		JsonNode root = mapper.readTree(responseBody);
		log.info("root:{}", root);
		JsonNode data = root.get("data");

		Organization organization = organizationRepository.findById(102L).get();

		Set<String> existingPostIds = postRepository.findPlatformUniqueIdsByPlatformAndOrganization("X", 102L);

		List<Post> posts = new ArrayList<>();

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

		if (data != null && data.isArray()) {
			for (JsonNode tweetNode : data) {
				if (!existingPostIds.contains(tweetNode.get("id").asText())) {
					Post post = parseTweetNode(tweetNode, organization, platform);
					posts.add(post);
					postRepository.saveAndFlush(post);

					JsonNode attachmentsNode = tweetNode.path("attachments");
					if (!attachmentsNode.isMissingNode()) {
						JsonNode mediaKeysNode = attachmentsNode.path("media_keys");
						if (mediaKeysNode.isArray()) {
							for (JsonNode mediaKeyNode : mediaKeysNode) {
								String mediaKey = mediaKeyNode.asText();
								JsonNode mediaInfo = mediaMap.get(mediaKey);
								if (mediaInfo != null) {
									PostMedia media = new PostMedia();
									media.setMediaType("video".equals(mediaInfo.path("type").asText()) ? "VIDEO" : "IMAGE");
									if ("VIDEO".equals(media.getMediaType())) {
										media.setFileUrl(getVideoUrlX(tweetNode.get("id").asText(), token));

										if(media.getFileUrl() == null){
											media.setFileUrl(mediaInfo.path("preview_image_url").asText());
										}

									} else {
										media.setFileUrl(mediaInfo.path("url").asText());
									}

									media.setPostId(post.getId());
									postMediaRepository.save(media);
								}
							}
						}
					}
				}
			}
		}

//		if(!posts.isEmpty()){
//			postRepository.saveAll(posts);
//		}
	}

	private Post parseTweetNode(JsonNode tweetNode, Organization organization, Platform platform) {
		Post post = new Post();

		post.setType("X");
		post.setPlatformUniqueId(tweetNode.get("id").asText());
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

		// TODO: Replace tweetNode.get("user_id").asText()
		User postedUser = getOrganizationPostedUser(organization.getId(), tweetNode.get("user_id").asText(), platform);
		if (postedUser != null) {
			post.setCreatedBy(postedUser);
		}

		return post;
	}

	private User getOrganizationPostedUser(Long id, String newExternalUserId, Platform platform) {
		Long defaultPostingUserId = 28L;

		Optional<UserAuthToken> userAuthFetch = userAuthTokenRepository.findFirstByUserIdExternalAndPlatformAndTenantId(newExternalUserId,platform,id);

		Optional<User> userFetch = userRepository.findById(userAuthFetch.isPresent()?userAuthFetch.get().getUserId() : defaultPostingUserId);
		return userFetch.orElse(null);
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

	private PostMedia parseContentEntity(JsonNode entityNode) {
		PostMedia entity = new PostMedia();
		entity.setFileUrl(getTextSafe(entityNode, "entityLocation"));
		return entity;
	}

	private String getTextSafe(JsonNode node, String fieldName) {
		if (node != null && node.has(fieldName) && !node.get(fieldName).isNull()) {
			return node.get(fieldName).asText();
		}
		return null;
	}

	public String getVideoUrlX(String tweetId, String token) {
		String url = String.format(
				"%s/tweets/%s?expansions=attachments.media_keys&media.fields=url,type,preview_image_url,variants",
				"https://api.twitter.com/2",
				tweetId
		);

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

	private String sanitizeFileName(String fileName) {
		return fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
	}

	private String getConfig(String key, String defaultValue) {
		return Optional.ofNullable(AppCache.configParameters.get(key))
				.map(ConfigParameter::getConfigValue)
				.filter(val -> !val.isEmpty())
				.orElse(defaultValue);
	}

	public static String cleanUnicode(String input) {
		if (input == null) return null;
		// Remove or replace problematic characters
		return input.replaceAll("[^\\x00-\\x7F]", "");
		// Or keep only basic printable ASCII
	}
}



