package com.social.ripple.external_ingestion.implementation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.social.ripple.external_ingestion.constants.ConfigKeys;
import com.social.ripple.external_ingestion.config.PublicUrlProperties;
import com.social.ripple.external_ingestion.dao.model.ExternalShare;
import com.social.ripple.external_ingestion.dao.model.Post;
import com.social.ripple.external_ingestion.dao.model.PostMedia;
import com.social.ripple.external_ingestion.dao.model.UserAuthToken;
import com.social.ripple.external_ingestion.dao.repository.*;
import com.social.ripple.external_ingestion.dto.request.TweetRequest;
import com.social.ripple.external_ingestion.dto.response.TweetResponse;
import com.social.ripple.external_ingestion.security.service.UserDetailsImpl;
import com.social.ripple.external_ingestion.service.IFacebookConnectionService;
import com.social.ripple.external_ingestion.service.MetaOAuthService;
import com.social.ripple.external_ingestion.service.OrganizationPlatformSettingsService;
import com.social.ripple.external_ingestion.util.PlatformBusinessPageComposer;
import com.social.ripple.external_ingestion.util.enumeration.AuthStatus;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oauth.signpost.OAuthConsumer;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacebookConnectionServiceImpl implements IFacebookConnectionService {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final UserAuthTokenRepository userAuthTokenRepository;
    private final PostRepository postRepository;
    private final ExternalShareRepository externalShareRepository;
    private final RestTemplate restTemplate;
    private final LoyaltyPointHelper loyaltyPointHelper;
    private final PostHashtagRepository postHashtagRepository;
    private final PostMediaRepository postMediaRepository;
    private final PublicUrlProperties publicUrlProperties;
    private final MetaOAuthService metaOAuthService;
    private final OrganizationPlatformSettingsService organizationPlatformSettingsService;

    public String instagramBaseUrl = "https://graph.instagram.com";
    public String FACEBOOK_BASE_URL = "https://graph.facebook.com/v19.0";

    @Override
    public UserAuthToken handleFbCallback(String oauthVerifier, UserAuthToken token, HttpSession session, String transactionId) {
        try {
            MetaOAuthService.MetaPersonalConnection connection = metaOAuthService
                    .exchangePersonalConnection(Platform.FACEBOOK, oauthVerifier, publicUrlProperties.getFacebookCallbackUrl());
            return saveUserTokens(connection.getExternalUserId(), connection.getAccessToken(), null, transactionId, Platform.FACEBOOK);
        } catch (Exception e) {
            throw new RuntimeException("Error handling OAuth callback: " + e.getMessage(), e);
        }
    }

    @Override
    public UserAuthToken handleInstagramCallback(String oauthVerifier, UserAuthToken token, HttpSession session, String transactionId) {
        try {
            MetaOAuthService.MetaPersonalConnection connection = metaOAuthService
                    .exchangePersonalConnection(Platform.INSTAGRAM, oauthVerifier, publicUrlProperties.getInstagramCallbackUrl());
            return saveUserTokens(connection.getExternalUserId(), connection.getAccessToken(), null, transactionId, Platform.INSTAGRAM);
        } catch (Exception e) {
            throw new RuntimeException("Error handling OAuth callback: " + e.getMessage(), e);
        }
    }



    private ExternalShare saveExternalShare(TweetRequest request, Long userId, Long tenantId, String externalPostId, Platform platform, String userName, String fbLink, String caption) {

        Integer points = 0;

        try {
            points = loyaltyPointHelper.handleLoyaltyPoints(request.getPostId(), userId, request.getType(), platform, userName);
        } catch (Exception e) {
            log.error("Loyalty point handling error:{}",e.getMessage());
        }
        ExternalShare externalShare = new ExternalShare();

        externalShare.setPostId(request.getPostId());
        externalShare.setPlatform(platform.toString());
        externalShare.setUserId(userId);
        if (StringUtils.hasText(caption)) {
            externalShare.setCaption(caption);
        }
        externalShare.setTenantId(tenantId);
        externalShare.setExternalPostId(externalPostId);
        externalShare.setSharedAt(LocalDateTime.now());
        externalShare.setPoint(points);

        if(fbLink!=null){
            externalShare.setFbLink(fbLink);
        }

        externalShareRepository.save(externalShare);

        return externalShare;
    }

    private String fetchExternalUserId(OAuthConsumer consumer) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(ConfigKeys.VERIFY_CREDENTIALS_URL);
            consumer.sign(request);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String jsonResponse = EntityUtils.toString(response.getEntity());
                JSONObject json = new JSONObject(jsonResponse);
                return json.getString("id_str");
            }
        }
    }

    @Override
    public UserAuthToken saveToken(UserAuthToken token) {
        return userAuthTokenRepository.save(token);
    }

    public UserAuthToken saveUserTokens(String externalUserId, String accessToken, String accessSecret, String transactionId, Platform platform) {
        Optional<UserAuthToken> existingTokenOpt = userAuthTokenRepository
                .findByTransactionIdAndPlatform(transactionId, platform);

        if (existingTokenOpt.isPresent()) {
            UserAuthToken token = existingTokenOpt.get();
            token.setUserIdExternal(externalUserId);
            token.setAccessToken(accessToken);
            token.setAccessSecret(accessSecret);
            token.setClientId(null);
            token.setIsConnected(true);
            token.setStatus(AuthStatus.CONNECTED);
            token.setUpdatedAt(LocalDateTime.now());
            token.setTransactionId(transactionId + "Exp");
            return userAuthTokenRepository.save(token);
        } else {
            UserAuthToken token = UserAuthToken.builder().userIdExternal(externalUserId).platform(platform)
                    .accessToken(accessToken).accessSecret(accessSecret).clientId(null)
                    .isConnected(true)
                    .status(AuthStatus.CONNECTED)
                    .createdAt(LocalDateTime.now())
                    .transactionId(transactionId + "Exp")
                    .build();

            return userAuthTokenRepository.save(token);
        }

    }

    private Post getPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id " + postId));
    }

    @Override
    public TweetResponse createFbPost(TweetRequest request, UserDetailsImpl userDetails) throws Exception {
        Post post = getPostById(request.getPostId());
        Platform platform = Platform.FACEBOOK;

        List<PostMedia> postMediaList = postMediaRepository.findByPostId(request.getPostId());

        Optional<UserAuthToken> userAuthTokenData = userAuthTokenRepository.findByUserIdAndPlatform(userDetails.getUserId(), platform);

        if (userAuthTokenData.isEmpty() || !StringUtils.hasText(userAuthTokenData.get().getAccessToken())) {
            TweetResponse err = new TweetResponse();
            err.setStatus(false);
            err.setMessage("No connected Facebook account found. Please connect your Facebook account in Settings first.");
            return err;
        }

        if (userAuthTokenData.isPresent()) {
            UserAuthToken userAuthToken = userAuthTokenData.get();

            if ("BUSINESS".equals(userAuthToken.getOauthSourcePage())) {
                TweetResponse blocked = new TweetResponse();
                blocked.setStatus(false);
                blocked.setMessage("Cannot post using business page connection. Please connect your personal account.");
                return blocked;
            }

            String userPageIdExternal = userAuthToken.getUserIdExternal();
            String accessToken = userAuthToken.getAccessToken();


            try {
                ObjectNode postRequest = mapper.createObjectNode();

                String content = StringUtils.hasText(request.getContent()) ? request.getContent() : post.getContent();

                List<String> hashTags = postHashtagRepository.fetchByPostId(request.getPostId());

                postRequest.put("access_token", accessToken);

                boolean isActualShare = false;

                if("SHARE".equals(request.getType())){
                    String fbLink = getPostExternalId(request.getPostId(),post,platform);

                    // Make it a share, only if the external post id is present
                    if(StringUtils.hasText(fbLink)){
                        // For shares with explicit link, skip business page suffix to avoid double link
                        if (hashTags != null && !hashTags.isEmpty()) {
                            content = content + "\n\n" + String.join(", ", hashTags);
                        }
                        PlatformBusinessPageComposer.validateLength(content, platform);
                        postRequest.put("message", content);
                        postRequest.put("link", fbLink);
                        isActualShare = true;
                    }
                }

                if (!isActualShare) {
                    content = organizationPlatformSettingsService.composeBusinessPageContent(
                            userDetails.getOrganization().getId(),
                            platform,
                            content
                    );
                    if (hashTags != null && !hashTags.isEmpty()) {
                        content = content + "\n\n" + String.join(", ", hashTags);
                    }
                    PlatformBusinessPageComposer.validateLength(content, platform);
                    postRequest.put("message", content);
                }



                List<String> imageContainerIds = new ArrayList<>();
                String videoUrl = null;


                if(("POST".equals(request.getType()) || !isActualShare) && postMediaList.size() >0) {

                    for (PostMedia postMedia : postMediaList) {

                        if ("IMAGE".equals(postMedia.getMediaType())) {
                            String containerId = createImageContainerFbFromUrl(postMedia.getFileUrl(),userPageIdExternal,accessToken);
                            imageContainerIds.add(containerId);
                            // Add delay to avoid rate limiting
                            try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        } else if ("VIDEO".equals(postMedia.getMediaType())) {
//                            videoContainerId = createVideoContainerFbFromUrl(postMedia.getFileUrl(), "Video content",userPageIdExternal,accessToken);
                            videoUrl = postMedia.getFileUrl();
                        }

                    }
                }

                JsonNode jsonResponse = null;

                if("POST".equals(request.getType()) || !isActualShare) {
                    if(videoUrl == null) {
                        jsonResponse = createFbPostInternal2(userPageIdExternal, content, accessToken, imageContainerIds);
                    }else {
                        jsonResponse = postVideoToFbPage(userPageIdExternal,content,accessToken,videoUrl);
                    }
                }else{
                    jsonResponse = createFbPostInternal(userPageIdExternal, postRequest,accessToken);
                }



                if(jsonResponse != null && jsonResponse.has("id")) {
                    Long userId = userDetails.getUserId();
                    String userName = userDetails.getUsername();
                    Long tenantId = userDetails.getOrganization().getId();
                    String externalPostId = jsonResponse.get("id").asText();

                    String fbLink = null;
                    if(jsonResponse.has("permalink_url")){
                        fbLink= jsonResponse.get("permalink_url").asText();
                    }

                    saveExternalShare(request, userId, tenantId, externalPostId, platform, userName, fbLink, content);

                    TweetResponse r = new TweetResponse();
                    r.setStatus(true);
                    return r;
                }


                return new TweetResponse();

            } catch (Exception e) {
                log.error("Error creating text post for user: {}", userPageIdExternal, e);
                throw new RuntimeException("Error creating text post", e);
            }



        } else {
            TweetResponse response = new TweetResponse();
            response.setStatus(false);
            response.setMessage("Facebook is not connected. Please connect your account in Settings.");
            return response;
        }
    }

    private String getPostExternalId(Long postId, Post post, Platform platform){
        String postExternalId = null;
        if(post.getType()!=null){
            if(post.getType().equals(platform.toString())){
                if(platform == Platform.FACEBOOK){
                    postExternalId = post.getFbLink();
                }else{
                    postExternalId = post.getPlatformUniqueId();
                }
            }else if(post.getType().equals("MANUAL") && post.getCreatedBy() !=null){
                ExternalShare externalShare = externalShareRepository.findByUserIdAndPostIdAndPlatform(post.getCreatedBy().getId(), postId,platform.toString());
                postExternalId = externalShare.getExternalPostId();
            }
        }

        return  postExternalId;
    }

    private JsonNode createFbPostInternal(String userPageId, JsonNode postRequest, String token) {
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;

        try {

            httpPost = new HttpPost("https://graph.facebook.com/v19.0/"+ userPageId +"/feed");
            httpPost.setEntity(new StringEntity(postRequest.toString(), ContentType.APPLICATION_JSON));
            httpPost.setHeader("Content-Type", "application/json");

            log.info("Creating Fb post for user: {}, Request: {}", userPageId, postRequest);

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                response = httpClient.execute(httpPost);
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                log.info("responseBody:{}",responseBody);

                if (statusCode == 200) {
                    JsonNode postResponse = mapper.readTree(responseBody);
                    log.info("Successfully created Fb post for user: {}, Post ID: {}",
                            userPageId, postResponse.get("id").asText());
                    return postResponse;
                } else {
                    log.error("Failed to create Fb post. Status: {}, Response: {}", statusCode, responseBody);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }




        } catch (Exception e) {
            log.error("IO error while creating Fb post for user: {}", userPageId, e);
        } finally {
            closeResources(response, httpPost);
        }

        return null;
    }

    private void closeResources(CloseableHttpResponse response, HttpRequestBase request) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException e) {
                log.warn("Error closing response", e);
            }
        }
        if (request != null) {
            request.releaseConnection();
        }
    }

    @Override
    public TweetResponse createInstagramPost(TweetRequest request, UserDetailsImpl userDetails) throws Exception {
        Post post = getPostById(request.getPostId());
        Platform platform = Platform.INSTAGRAM;

        List<PostMedia> postMediaList = postMediaRepository.findByPostId(request.getPostId());

        Optional<UserAuthToken> userAuthTokenData = userAuthTokenRepository.findByUserIdAndPlatform(userDetails.getUserId(), platform);

        if (userAuthTokenData.isPresent()) {
            UserAuthToken userAuthToken = userAuthTokenData.get();

            if ("BUSINESS".equals(userAuthToken.getOauthSourcePage())) {
                TweetResponse blocked = new TweetResponse();
                blocked.setStatus(false);
                blocked.setMessage("Cannot post using business page connection. Please connect your personal account.");
                return blocked;
            }

            String userPageIdExternal = userAuthToken.getUserIdExternal();
            String accessToken = userAuthToken.getAccessToken();


            try {
                ObjectNode postRequest = mapper.createObjectNode();

                String content = StringUtils.hasText(request.getContent()) ? request.getContent() : post.getContent();

                List<String> hashTags = postHashtagRepository.fetchByPostId(request.getPostId());

                content = organizationPlatformSettingsService.composeBusinessPageContent(
                        userDetails.getOrganization().getId(),
                        platform,
                        content
                );
                if (hashTags != null && !hashTags.isEmpty()) {
                    content = content + "\n\n" + String.join(", ", hashTags);
                }
                PlatformBusinessPageComposer.validateLength(content, platform);

//                postRequest.put("message", content);
//                postRequest.put("access_token", accessToken);
//
//                if("SHARE".equals(request.getType())){
//                    String fbLink = getPostExternalId(request.getPostId(),post,platform);
//
//                    // Make it a share, only if the external post id is present
//                    if(StringUtils.hasText(fbLink)){
//                        if(StringUtils.hasText(request.getContent())) {
//                            postRequest.put("message", request.getContent());
//                        }
//
//                        postRequest.put("link", fbLink);
//                    }
//                }


                JsonNode jsonResponse = createInstagramPostInternal(post, postMediaList, content, userPageIdExternal, accessToken);

                if(jsonResponse != null && jsonResponse.has("id")) {
                    Long userId = userDetails.getUserId();
                    String userName = userDetails.getUsername();
                    Long tenantId = userDetails.getOrganization().getId();
                    String externalPostId = jsonResponse.get("id").asText();

                    saveExternalShare(request, userId, tenantId, externalPostId, platform, userName, null, content);

                    TweetResponse r = new TweetResponse();
                    r.setStatus(true);
                    return r;
                }


                return new TweetResponse();

            } catch (Exception e) {
                log.error("Error creating text post for user: {}", userPageIdExternal, e);
                throw new RuntimeException("Error creating text post", e);
            }



        } else {
            TweetResponse response = new TweetResponse();
            response.setStatus(false);
            response.setMessage("Instagram is not connected. Please connect your account in Settings.");
            return response;
        }
    }

    private JsonNode createInstagramPostInternal(Post post, List<PostMedia> postMediaList, String content, String userPageIdExternal, String accessToken) {
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;

        try {

            // Get post media
            if(!postMediaList.isEmpty()) {
                PostMedia imageMedia = postMediaList.getFirst();

                String mediaType = "VIDEO".equals(imageMedia.getMediaType())?"REELS":"IMAGE";

                JsonNode containerResponse = createMediaContainer(imageMedia.getFileUrl(), content,userPageIdExternal, accessToken, mediaType);
                String creationId = containerResponse.get("id").asText();

                // Wait for processing — poll status instead of fixed sleep
                String containerStatus = pollContainerStatus(creationId, accessToken, mediaType);
                if (!"FINISHED".equals(containerStatus)) {
                    log.error("Instagram media container {} not ready after polling. Status: {}", creationId, containerStatus);
                    return null;
                }

                // Step 2: Publish container
                return publishContainer(creationId, userPageIdExternal, accessToken);


            }


        } catch (Exception e) {
            log.error("IO error while creating Fb post for user: {}", userPageIdExternal, e);
        } finally {
            closeResources(response, httpPost);
        }

        return null;
    }

    public JsonNode createMediaContainer(String imageUrl, String caption, String externalPageId, String accessToken, String mediaType) throws IOException, URISyntaxException {
        log.info("Instagram container creation | mediaType={} | imageUrl={} | externalPageId={}", mediaType, imageUrl, externalPageId);
        if (imageUrl == null || imageUrl.isBlank()) {
            log.error("Instagram container creation skipped — image URL is empty | externalPageId={}", externalPageId);
            throw new IOException("Instagram media URL is empty — cannot create container");
        }
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            List<NameValuePair> formParams = new ArrayList<>();
            formParams.add(new BasicNameValuePair("access_token", accessToken));
            if("REELS".equals(mediaType)){
                formParams.add(new BasicNameValuePair("video_url", imageUrl));
            }else {
                formParams.add(new BasicNameValuePair("image_url", imageUrl));
            }
            formParams.add(new BasicNameValuePair("caption", caption));
            formParams.add(new BasicNameValuePair("media_type", mediaType));

            // Create HTTP POST request
            HttpPost httpPost = new HttpPost("https://graph.facebook.com/v24.0/" + externalPageId + "/media");
            httpPost.setEntity(new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.info("Container creation response: {}", responseBody);
                return mapper.readTree(responseBody);
            }
        }
    }

    private String pollContainerStatus(String containerId, String accessToken, String mediaType) {
        int maxAttempts = "REELS".equals(mediaType) ? 18 : 6; // 3 min for video, 1 min for image
        int sleepMs = 10000; // 10 seconds between polls
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Thread.sleep(sleepMs);
                String url = FACEBOOK_BASE_URL + "/" + containerId + "?fields=status_code&access_token=" + accessToken;
                try (CloseableHttpClient client = HttpClients.createDefault()) {
                    HttpGet request = new HttpGet(url);
                    try (CloseableHttpResponse response = client.execute(request)) {
                        String body = EntityUtils.toString(response.getEntity());
                        JsonNode node = new ObjectMapper().readTree(body);
                        String status = node.path("status_code").asText("IN_PROGRESS");
                        log.info("Container {} status poll {}/{}: {}", containerId, attempt, maxAttempts, status);
                        if ("FINISHED".equals(status)) {
                            return "FINISHED";
                        }
                        if ("ERROR".equals(status)) {
                            return "ERROR";
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Container status poll failed for {}: {}", containerId, e.getMessage());
            }
        }
        return "TIMEOUT";
    }

    public JsonNode publishContainer(String creationId, String externalPageId ,String accessToken) throws IOException, URISyntaxException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            List<NameValuePair> formParams = new ArrayList<>();
            formParams.add(new BasicNameValuePair("access_token", accessToken));
            formParams.add(new BasicNameValuePair("creation_id", creationId));

            // Create HTTP POST request
            HttpPost httpPost = new HttpPost("https://graph.facebook.com/v24.0/" + externalPageId + "/media_publish");
            httpPost.setEntity(new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setHeader("Accept", "application/json");



            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.info("Publish response: {}", responseBody);
                return mapper.readTree(responseBody);
            }
        }
    }

    public String createImageContainerFbFromUrl(String imageUrl, String userPageIdExternal, String accessToken) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            HttpPost uploadRequest = new HttpPost(FACEBOOK_BASE_URL + "/" + userPageIdExternal + "/photos");

            // Build form parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("published", "false"));
            params.add(new BasicNameValuePair("url", imageUrl)); // Direct URL upload

            uploadRequest.setEntity(new UrlEncodedFormEntity(params));
            uploadRequest.setHeader("Authorization", "Bearer " + accessToken);

            CloseableHttpResponse response = httpClient.execute(uploadRequest);
            HttpEntity responseEntity = response.getEntity();

            String responseString = EntityUtils.toString(responseEntity, "UTF-8");
            log.info("img responseString:{}",responseString);
            EntityUtils.consume(responseEntity);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(responseString);

            if (jsonResponse.has("error")) {
                throw new IOException("Facebook API Error: " + jsonResponse.get("error").toString());
            }

            String imageId = jsonResponse.get("id").asText();
            response.close();

            return imageId;

        } finally {
            httpClient.close();
        }
    }

    public String createVideoContainerFbFromUrl(String videoUrl, String description, String userPageIdExternal, String accessToken) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            HttpPost uploadRequest = new HttpPost(FACEBOOK_BASE_URL + "/" + userPageIdExternal + "/videos");

            // Build form parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("published", "false"));
            params.add(new BasicNameValuePair("file_url", videoUrl)); // Video uses file_url parameter
            params.add(new BasicNameValuePair("description", description != null ? description : "Video upload"));

            uploadRequest.setEntity(new UrlEncodedFormEntity(params));
            uploadRequest.setHeader("Authorization", "Bearer " + accessToken);

            CloseableHttpResponse response = httpClient.execute(uploadRequest);
            HttpEntity responseEntity = response.getEntity();

            String responseString = EntityUtils.toString(responseEntity, "UTF-8");
            log.info("video responseString:{}",responseString);
            EntityUtils.consume(responseEntity);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(responseString);

            if (jsonResponse.has("error")) {
                throw new IOException("Facebook API Error: " + jsonResponse.get("error").toString());
            }

            String videoId = jsonResponse.get("id").asText();
            response.close();

            return videoId;

        } finally {
            httpClient.close();
        }
    }

    private JsonNode createFbPostInternal2(String userPageId, String content, String accessToken, List<String> imageContainerIds) {


        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            HttpPost createPost = new HttpPost(FACEBOOK_BASE_URL + "/" + userPageId + "/feed");

            // Build request parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("message", content));

            // Add image containers
            if (imageContainerIds != null && !imageContainerIds.isEmpty()) {
                for (int i = 0; i < imageContainerIds.size(); i++) {
                    String mediaJson = "{\"media_fbid\":\"" + imageContainerIds.get(i) + "\"}";
                    params.add(new BasicNameValuePair("attached_media[" + i + "]", mediaJson));
                }
            }

            // Add video container
//            if (videoContainerId != null && !videoContainerId.isEmpty()) {
//                int index = (imageContainerIds != null ? imageContainerIds.size() : 0);
//                String videoJson = "{\"media_fbid\":\"" + videoContainerId + "\"}";
//                params.add(new BasicNameValuePair("attached_media[" + index + "]", videoJson));
//            }

            createPost.setEntity(new UrlEncodedFormEntity(params));
            createPost.setHeader("Authorization", "Bearer " + accessToken);

            CloseableHttpResponse response = httpClient.execute(createPost);
            HttpEntity responseEntity = response.getEntity();

            String responseString = EntityUtils.toString(responseEntity, "UTF-8");
            log.info("responseString: {}",responseString);
            EntityUtils.consume(responseEntity);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(responseString);

            if (jsonResponse.has("error")) {
                throw new IOException("Facebook API Error: " + jsonResponse.get("error").toString());
            }

            response.close();

            return jsonResponse;

        } catch (Exception e) {
            log.info("error: {}",e);
        } finally {
        }


        return null;
    }

    public JsonNode postVideoToFbPage(String pageId, String content,String accessToken,String videoUrl) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            HttpPost postRequest = new HttpPost(FACEBOOK_BASE_URL + "/" + pageId + "/videos");

            // Build parameters
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("file_url", videoUrl));
            params.add(new BasicNameValuePair("description", content));
            params.add(new BasicNameValuePair("published", "true"));

            postRequest.setEntity(new UrlEncodedFormEntity(params));

            postRequest.setHeader("Authorization", "Bearer " + accessToken);

            // Execute request
            CloseableHttpResponse response = httpClient.execute(postRequest);
            HttpEntity responseEntity = response.getEntity();

            String responseString = EntityUtils.toString(responseEntity, "UTF-8");
            log.info("video response: {}", responseString);
            EntityUtils.consume(responseEntity);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonResponse = mapper.readTree(responseString);

            if (jsonResponse.has("error")) {
                throw new IOException("Facebook API Error: " + jsonResponse.get("error").get("message").asText());
            }

            response.close();

            return jsonResponse;

        } finally {
            httpClient.close();
        }
    }

}
