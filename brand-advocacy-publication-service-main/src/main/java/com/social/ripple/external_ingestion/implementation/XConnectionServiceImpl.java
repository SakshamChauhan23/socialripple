package com.social.ripple.external_ingestion.implementation;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.social.ripple.external_ingestion.dao.model.*;
import com.social.ripple.external_ingestion.dao.repository.*;
import com.social.ripple.external_ingestion.dto.request.TweetRequest;
import com.social.ripple.external_ingestion.dto.request.UploadPart;
import com.social.ripple.external_ingestion.dto.response.*;
import com.social.ripple.external_ingestion.config.PublicUrlProperties;
import com.social.ripple.external_ingestion.security.service.UserDetailsImpl;
import com.social.ripple.external_ingestion.util.enumeration.AuthStatus;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import com.social.ripple.external_ingestion.constants.ConfigKeys;
import com.social.ripple.external_ingestion.service.OrgBusinessPageContentService;
import com.social.ripple.external_ingestion.service.XConnectionService;
import com.social.ripple.external_ingestion.util.PlatformBusinessPageComposer;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import com.social.ripple.external_ingestion.util.enumeration.TransactionStatus;

import lombok.RequiredArgsConstructor;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.http.HttpParameters;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class XConnectionServiceImpl implements XConnectionService {
    private static final String META_GRAPH_BASE_URL = "https://graph.facebook.com/v24.0";
    private static final String LINKEDIN_KEY = Platform.LINKEDIN.toString();
    private static final String FACEBOOK_KEY = Platform.FACEBOOK.toString();
    private static final String INSTAGRAM_KEY = Platform.INSTAGRAM.toString();
    private static final String X_KEY = Platform.X.toString();

    @Value("${twitter.consumer.key}")
    private String consumerKey;

    @Value("${twitter.consumer.secret}")
    private String consumerSecret;

    @Value("${app.oauth.linkedin-client-id:}")
    private String linkedInClientId;

    @Value("${app.oauth.linkedin-client-secret:}")
    private String linkedInClientSecret;

    @Value("${app.oauth.linkedin-org-client-id:}")
    private String linkedInOrgClientId;

    @Value("${app.oauth.linkedin-org-client-secret:}")
    private String linkedInOrgClientSecret;

    private static final ObjectMapper mapper = new ObjectMapper();

    private final UserAuthTokenRepository userAuthTokenRepository;
    private final PostRepository postRepository;
    private final ExternalShareRepository externalShareRepository;
    private final ShareAnalyticsRepository shareAnalyticsRepository;
    private final PostMediaRepository postMediaRepository;
    private final RestTemplate restTemplate;
    private final LoyaltyPointHelper loyaltyPointHelper;
    private final LinkedinHelper linkedinHelper;
    private final PostHashtagRepository postHashtagRepository;
    private final com.social.ripple.external_ingestion.dao.repository.ExternalPlatformRepository externalPlatformRepository;
    private final PublicUrlProperties publicUrlProperties;
    private final OrgBusinessPageContentService orgBusinessPageContentService;

    static final class AnalyticsReference {
        private final Platform platform;
        private final Long userId;
        private final String externalPostId;
        private final LocalDateTime capturedAt;
        private final Long organizationId;
        private final boolean useOrgToken;

        AnalyticsReference(Platform platform, Long userId, String externalPostId, LocalDateTime capturedAt) {
            this(platform, userId, externalPostId, capturedAt, null, false);
        }

        AnalyticsReference(Platform platform, Long userId, String externalPostId, LocalDateTime capturedAt, Long organizationId, boolean useOrgToken) {
            this.platform = platform;
            this.userId = userId;
            this.externalPostId = externalPostId;
            this.capturedAt = capturedAt;
            this.organizationId = organizationId;
            this.useOrgToken = useOrgToken;
        }
    }

    static final class AnalyticsSnapshot {
        final int impressions;
        final int reach;
        final int likes;
        final int comments;
        final int shares;
        final int saves;
        final int bookmarks;
        final int retweets;
        final int replies;
        final int quotes;
        final int clicks;
        final int videoViews;

        // Derived
        final int engagements;

        private AnalyticsSnapshot(int impressions, int reach, int likes, int comments, int shares,
                                  int saves, int bookmarks, int retweets, int replies, int quotes,
                                  int clicks, int videoViews) {
            this.impressions = impressions;
            this.reach = reach;
            this.likes = likes;
            this.comments = comments;
            this.shares = shares;
            this.saves = saves;
            this.bookmarks = bookmarks;
            this.retweets = retweets;
            this.replies = replies;
            this.quotes = quotes;
            this.clicks = clicks;
            this.videoViews = videoViews;
            this.engagements = likes + comments + shares + saves + retweets + replies + quotes;
        }
    }

    @Override
    public String getAuthorizationUrl(String callbackUrl, UserAuthToken token, HttpSession session) {
        try {
            OAuthConsumer consumer = new CommonsHttpOAuthConsumer(consumerKey,
                    consumerSecret);
            OAuthProvider provider = new CommonsHttpOAuthProvider(
                    ConfigKeys.REQUEST_TOKEN_URL,
                    ConfigKeys.ACCESS_TOKEN_URL,
                    ConfigKeys.AUTHORIZE_URL
            );

            String authorizationUrl = provider.retrieveRequestToken(consumer, callbackUrl);
            token.setAccessToken(consumer.getToken());
            token.setAccessSecret(consumer.getTokenSecret());
            token.setTransactionStatus(TransactionStatus.ACTIVE);
            token.setUpdatedAt(LocalDateTime.now());
            userAuthTokenRepository.save(token);

            session.setAttribute("consumer", consumer);
            session.setAttribute("provider", provider);

            return authorizationUrl;

        } catch (Exception e) {
            throw new RuntimeException("Error generating OAuth authorization URL: " + e.getMessage(), e);
        }
    }

    @Override
    public OrgAuthorizationStart beginOrganizationAuthorization(String callbackUrl, HttpSession session) {
        try {
            OAuthConsumer consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
            OAuthProvider provider = new CommonsHttpOAuthProvider(
                    ConfigKeys.REQUEST_TOKEN_URL,
                    ConfigKeys.ACCESS_TOKEN_URL,
                    ConfigKeys.AUTHORIZE_URL
            );

            String authorizationUrl = provider.retrieveRequestToken(consumer, callbackUrl);
            session.setAttribute("org_x_consumer", consumer);
            session.setAttribute("org_x_provider", provider);
            return new OrgAuthorizationStart(
                    authorizationUrl,
                    consumer.getToken(),
                    consumer.getTokenSecret()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error generating OAuth authorization URL: " + e.getMessage(), e);
        }
    }

    @Override
    public UserAuthToken handleCallback(String oauthVerifier, UserAuthToken token, HttpSession session, String transactionId) {
        try {
            OAuthConsumer consumer = (OAuthConsumer) session.getAttribute("consumer");
            OAuthProvider provider = (OAuthProvider) session.getAttribute("provider");

            if (consumer == null || provider == null) {
                if (!StringUtils.hasText(token.getAccessToken()) || !StringUtils.hasText(token.getAccessSecret())) {
                    throw new RuntimeException("X OAuth request token state missing");
                }
                consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
                consumer.setTokenWithSecret(token.getAccessToken(), token.getAccessSecret());
                provider = new CommonsHttpOAuthProvider(
                        ConfigKeys.REQUEST_TOKEN_URL,
                        ConfigKeys.ACCESS_TOKEN_URL,
                        ConfigKeys.AUTHORIZE_URL
                );
            }

            provider.retrieveAccessToken(consumer, oauthVerifier);

            String accessToken = consumer.getToken();
            String accessSecret = consumer.getTokenSecret();

            // Extract user_id from access token response (Twitter includes it)
            // Fall back to verify_credentials if not available
            HttpParameters responseParams = provider.getResponseParameters();
            String externalUserId = responseParams != null ? responseParams.getFirst("user_id") : null;
            if (!StringUtils.hasText(externalUserId)) {
                log.info("X user_id not in token response, falling back to verify_credentials");
                externalUserId = fetchExternalUserId(consumer);
            }
            log.info("X personal OAuth completed | externalUserId={} | transactionId={}", externalUserId, transactionId);

            try {
                validateXReadAccess(externalUserId, accessToken, accessSecret);
            } catch (Exception readEx) {
                log.warn("X read access validation failed (non-fatal) | externalUserId={} | {}", externalUserId, readEx.getMessage());
            }

            return saveUserTokens(externalUserId, accessToken, accessSecret, transactionId, Platform.X);

        } catch (Exception e) {
            throw new RuntimeException("Error handling OAuth callback: " + e.getMessage(), e);
        }
    }

    @Override
    public UserAuthToken handleLinkedInCallback(String oauthVerifier, UserAuthToken token, HttpSession session, String transactionId) {
        try {
            // Determine which LinkedIn app credentials to use
            boolean isLeaderConnection = "LEADER_LINKEDIN".equals(token.getClientId());
            String effectiveClientId = isLeaderConnection ? linkedInOrgClientId : linkedInClientId;
            String effectiveClientSecret = isLeaderConnection ? linkedInOrgClientSecret : linkedInClientSecret;
            log.info("LinkedIn callback | transactionId={} | isLeader={} | appSuffix={}",
                    transactionId, isLeaderConnection,
                    effectiveClientId != null && effectiveClientId.length() > 4 ? effectiveClientId.substring(effectiveClientId.length() - 4) : effectiveClientId);

            if (!StringUtils.hasText(effectiveClientId) || !StringUtils.hasText(effectiveClientSecret)) {
                log.error("LinkedIn OAuth credentials missing | isLeader={} | clientIdConfigured={} | clientSecretConfigured={}",
                        isLeaderConnection, StringUtils.hasText(effectiveClientId), StringUtils.hasText(effectiveClientSecret));
                throw new RuntimeException("LinkedIn client credentials are not configured");
            }

            String accessToken = null;
            String externalUserId = null;
            String redirectUri = publicUrlProperties.getLinkedInCallbackUrl();


            try {
                // Build form parameters
                List<NameValuePair> formParams = new ArrayList<>();
                formParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
                formParams.add(new BasicNameValuePair("code", oauthVerifier));
                formParams.add(new BasicNameValuePair("redirect_uri", redirectUri));
                formParams.add(new BasicNameValuePair("client_id", effectiveClientId));
                formParams.add(new BasicNameValuePair("client_secret", effectiveClientSecret));

                // Create HTTP POST request
                HttpPost httpPost = new HttpPost("https://www.linkedin.com/oauth/v2/accessToken");
                httpPost.setEntity(new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8));
                httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
                httpPost.setHeader("Accept", "application/json");

                log.info("Starting LinkedIn personal token exchange | transactionId={} | redirectUri={} | clientIdSuffix={}",
                        transactionId,
                        redirectUri,
                        linkedInClientId.length() > 4 ? linkedInClientId.substring(linkedInClientId.length() - 4) : linkedInClientId);

                try (CloseableHttpClient httpClient = HttpClients.createDefault();
                        CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    HttpEntity entity = response.getEntity();
                    String responseBody = EntityUtils.toString(entity);
                    EntityUtils.consume(entity);

                    if (statusCode == 200) {
                        JsonNode jsonResponse = mapper.readTree(responseBody);
                        accessToken = jsonResponse.get("access_token").asText();
                        int expiresIn = jsonResponse.get("expires_in").asInt();

                        log.info("LinkedIn personal token exchange succeeded | transactionId={} | expiresIn={}s",
                                transactionId, expiresIn);
                    } else {
                        log.error("LinkedIn personal token exchange failed | transactionId={} | status={} | redirectUri={} | response={}",
                                transactionId, statusCode, redirectUri, responseBody);
                        throw new RuntimeException("LinkedIn token exchange failed");
                    }
                }


                // Get user identity — use /v2/userinfo for personal (App 1), organizationAuthorizations for leader (App 2)
                if (isLeaderConnection) {
                    // App 2 doesn't have Sign In product — resolve person ID from organizationAuthorizations impersonator field
                    String orgAuthUrl = "https://api.linkedin.com/rest/organizationAuthorizations"
                            + "?bq=authorizationActionsAndImpersonator"
                            + "&authorizationActions=List("
                            + "(authorizationAction:(organizationProfileAuthorizationAction:(actionType:ADMINISTRATION_PAGE_VIEW))),"
                            + "(authorizationAction:(organizationContentAuthorizationAction:(actionType:ORGANIC_SHARE_CREATE)))"
                            + ")";
                    HttpGet httpGet = new HttpGet(orgAuthUrl);
                    httpGet.setHeader("Authorization", "Bearer " + accessToken);
                    httpGet.setHeader("Accept", "application/json");
                    httpGet.setHeader("X-Restli-Protocol-Version", "2.0.0");
                    httpGet.setHeader("Linkedin-Version", "202603");

                    try (CloseableHttpClient httpClient2 = HttpClients.createDefault();
                         CloseableHttpResponse response2 = httpClient2.execute(httpGet)) {
                        int statusCode2 = response2.getStatusLine().getStatusCode();
                        String responseBody2 = EntityUtils.toString(response2.getEntity());

                        if (statusCode2 == 200) {
                            JsonNode jsonResponse2 = mapper.readTree(responseBody2);
                            String impersonatorUrn = null;
                            JsonNode outerElements = jsonResponse2.path("elements");
                            if (outerElements.isArray()) {
                                for (JsonNode actionBucket : outerElements) {
                                    JsonNode innerElements = actionBucket.path("elements");
                                    if (innerElements.isArray()) {
                                        for (JsonNode element : innerElements) {
                                            JsonNode impNode = element.get("impersonator");
                                            if (impNode != null && !impNode.isNull() && StringUtils.hasText(impNode.asText())) {
                                                impersonatorUrn = impNode.asText().trim();
                                                break;
                                            }
                                        }
                                    }
                                    if (impersonatorUrn != null) break;
                                }
                            }

                            if (impersonatorUrn != null && impersonatorUrn.startsWith("urn:li:person:")) {
                                externalUserId = impersonatorUrn.replace("urn:li:person:", "");
                                log.info("LinkedIn leader identity resolved via organizationAuthorizations | transactionId={} | externalUserId={}", transactionId, externalUserId);
                            } else {
                                log.error("LinkedIn leader identity resolution failed — no impersonator URN found | transactionId={} | response={}", transactionId, responseBody2);
                                throw new RuntimeException("LinkedIn leader profile: unable to resolve person identity from organizationAuthorizations");
                            }
                        } else {
                            log.error("LinkedIn leader organizationAuthorizations failed | transactionId={} | status={} | response={}", transactionId, statusCode2, responseBody2);
                            throw new RuntimeException("LinkedIn leader organization authorizations request failed with HTTP " + statusCode2);
                        }
                    }
                } else {
                    HttpGet httpGet = new HttpGet("https://api.linkedin.com/v2/userinfo");
                    httpGet.setHeader("Authorization", "Bearer " + accessToken);

                    try (CloseableHttpClient httpClient2 = HttpClients.createDefault();
                         CloseableHttpResponse response2 = httpClient2.execute(httpGet)) {
                        int statusCode2 = response2.getStatusLine().getStatusCode();
                        String responseBody2 = EntityUtils.toString(response2.getEntity());

                        if (statusCode2 == 200) {
                            JsonNode jsonResponse2 = mapper.readTree(responseBody2);
                            externalUserId = jsonResponse2.get("sub").asText();
                            log.info("LinkedIn personal userinfo succeeded | transactionId={} | externalUserId={}", transactionId, externalUserId);
                        } else {
                            log.error("LinkedIn personal userinfo failed | transactionId={} | status={} | response={}", transactionId, statusCode2, responseBody2);
                            throw new RuntimeException("LinkedIn user profile request failed");
                        }
                    }
                }
                try {
                    validateLinkedInReadAccess(externalUserId, accessToken, transactionId);
                } catch (Exception readEx) {
                    log.warn("LinkedIn read access validation failed (non-fatal) | transactionId={} | externalUserId={} | {}",
                            transactionId, externalUserId, readEx.getMessage());
                }
            } catch (IOException e) {
                log.error("IO error during LinkedIn personal OAuth callback | transactionId={}", transactionId, e);
                throw new RuntimeException("Error handling OAuth callback: " + e.getMessage(), e);
            } finally {
            }

            if(accessToken !=null){
                return saveUserTokens(externalUserId, accessToken, null, transactionId, Platform.LINKEDIN);
            }else {
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Error handling OAuth callback: " + e.getMessage(), e);
        }
    }

    @Override
    public OrgAccountConnection exchangeOrganizationAccount(String oauthVerifier, String requestToken, String requestTokenSecret, HttpSession session) {
        try {
            if (!StringUtils.hasText(requestToken) || !StringUtils.hasText(requestTokenSecret)) {
                throw new RuntimeException("X OAuth transaction state missing");
            }
            log.info("X org OAuth exchange starting | requestTokenPresent=true | verifierPresent={}", StringUtils.hasText(oauthVerifier));

            // Exchange request token for access token via direct HTTP call
            // (signpost's CommonsHttpOAuthProvider fails when recreated from stored tokens)
            OAuthConsumer consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
            consumer.setTokenWithSecret(requestToken, requestTokenSecret);

            org.apache.http.client.methods.HttpPost accessTokenRequest = new org.apache.http.client.methods.HttpPost(ConfigKeys.ACCESS_TOKEN_URL);
            accessTokenRequest.setEntity(new org.apache.http.entity.StringEntity("oauth_verifier=" + oauthVerifier, org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED));
            consumer.sign(accessTokenRequest);

            String responseBody;
            try (org.apache.http.impl.client.CloseableHttpClient httpClient = org.apache.http.impl.client.HttpClients.createDefault();
                 org.apache.http.client.methods.CloseableHttpResponse httpResponse = httpClient.execute(accessTokenRequest)) {
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                responseBody = org.apache.http.util.EntityUtils.toString(httpResponse.getEntity());
                if (statusCode != 200) {
                    log.error("X org access token exchange HTTP {} | body={}", statusCode, responseBody);
                    throw new RuntimeException("X access token exchange failed with HTTP " + statusCode + ": " + responseBody);
                }
            }
            log.info("X org OAuth access token exchange succeeded");

            // Parse response: oauth_token=...&oauth_token_secret=...&user_id=...&screen_name=...
            java.util.Map<String, String> params = new java.util.HashMap<>();
            for (String pair : responseBody.split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    params.put(java.net.URLDecoder.decode(kv[0], java.nio.charset.StandardCharsets.UTF_8),
                               java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8));
                }
            }

            String accessToken = params.get("oauth_token");
            String accessSecret = params.get("oauth_token_secret");

            // Extract user_id/screen_name from access token response
            String externalUserId = params.get("user_id");
            String username = params.get("screen_name");
            String displayName = username;

            // Fall back to verify_credentials if token response lacks user_id
            if (!StringUtils.hasText(externalUserId)) {
                log.info("X org user_id not in token response, falling back to verify_credentials");
                try {
                    OAuthConsumer accessConsumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
                    accessConsumer.setTokenWithSecret(accessToken, accessSecret);
                    JSONObject identity = fetchXAccountIdentity(accessConsumer);
                    externalUserId = identity.optString("id_str", null);
                    username = identity.optString("screen_name", username);
                    displayName = identity.optString("name", username);
                } catch (Exception identityEx) {
                    log.warn("X org verify_credentials failed (non-fatal): {}", identityEx.getMessage());
                }
            }
            log.info("X org OAuth identity resolved | externalUserId={} | username={}", externalUserId, username);

            if (!StringUtils.hasText(accessToken) || !StringUtils.hasText(accessSecret) || !StringUtils.hasText(externalUserId)) {
                log.error("X org OAuth incomplete credentials | accessTokenPresent={} | accessSecretPresent={} | externalUserId={}",
                        StringUtils.hasText(accessToken), StringUtils.hasText(accessSecret), externalUserId);
                throw new RuntimeException("Incomplete X account credentials returned from OAuth");
            }

            return new OrgAccountConnection(
                    externalUserId,
                    accessToken,
                    accessSecret,
                    username,
                    displayName,
                    StringUtils.hasText(username) ? "https://x.com/" + username : null
            );
        } catch (Exception e) {
            log.error("X org OAuth exchange failed: {}", e.getMessage(), e);
            throw new RuntimeException("Error handling organization OAuth callback: " + e.getMessage(), e);
        }
    }

    @Override
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
            token.setTransactionId(transactionId);
            return userAuthTokenRepository.save(token);
        } else {
            UserAuthToken token = UserAuthToken.builder().userIdExternal(externalUserId).platform(platform)
                    .accessToken(accessToken).accessSecret(accessSecret).clientId(null)
                    .isConnected(true)
                    .status(AuthStatus.CONNECTED)
                    .createdAt(LocalDateTime.now())
                    .transactionId(transactionId)
                    .build();

            return userAuthTokenRepository.save(token);
        }

    }

    @Override
    public UserAuthToken saveToken(UserAuthToken token) {
        return userAuthTokenRepository.save(token);
    }

    @Override
    public UserAuthToken getTokenByTransactionId(String transactionId) {
        return userAuthTokenRepository.findByTransactionId(transactionId).orElse(null); // Controller will handle null
        // with proper BaseResponse
    }

    @Override
    public UserAuthToken getPendingTokenByRequestToken(String requestToken) {
        return userAuthTokenRepository
                .findByAccessTokenAndPlatformAndTransactionStatus(requestToken, Platform.X, TransactionStatus.ACTIVE)
                .orElse(null);
    }

    private String fetchExternalUserId(OAuthConsumer consumer) throws Exception {
        return fetchXAccountIdentity(consumer).getString("id_str");
    }

    private void validateXReadAccess(String externalUserId, String accessToken, String accessSecret) throws Exception {
        if (!StringUtils.hasText(externalUserId) || !StringUtils.hasText(accessToken) || !StringUtils.hasText(accessSecret)) {
            throw new RuntimeException("Incomplete X credentials returned from OAuth");
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String url = "https://api.twitter.com/2/users/" + externalUserId + "/tweets?max_results=5&exclude=retweets,replies";
            HttpGet request = new HttpGet(url);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("User-Agent", "TwitterJavaClient/1.0");

            OAuthConsumer consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
            consumer.setTokenWithSecret(accessToken, accessSecret);
            consumer.sign(request);

            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                if (statusCode != 200) {
                    log.error("X personal read validation failed | externalUserId={} | status={} | response={}",
                            externalUserId, statusCode, responseBody);
                    throw new RuntimeException("X read validation failed: HTTP " + statusCode);
                }
            }
        }
    }

    private void validateLinkedInReadAccess(String externalUserId, String accessToken, String transactionId) throws IOException {
        if (!StringUtils.hasText(externalUserId) || !StringUtils.hasText(accessToken)) {
            throw new RuntimeException("Incomplete LinkedIn credentials returned from OAuth");
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String apiUrl = "https://api.linkedin.com/rest/posts?q=author&author=urn%3Ali%3Aperson%3A"
                    + externalUserId
                    + "&count=1";
            HttpGet request = new HttpGet(apiUrl);
            request.setHeader("Authorization", "Bearer " + accessToken);
            request.setHeader("X-Restli-Protocol-Version", "2.0.0");
            request.setHeader("LinkedIn-Version", "202509");

            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                if (statusCode == 200) {
                    return;
                }
                log.warn("LinkedIn personal read validation primary endpoint failed | transactionId={} | externalUserId={} | status={} | response={}",
                        transactionId, externalUserId, statusCode, responseBody);
            }

            String fallbackUrl = "https://api.linkedin.com/v2/shares?q=owners&owners=urn:li:person:" + externalUserId + "&count=1";
            HttpGet fallbackRequest = new HttpGet(fallbackUrl);
            fallbackRequest.setHeader("Authorization", "Bearer " + accessToken);
            fallbackRequest.setHeader("Content-Type", "application/json");

            try (CloseableHttpResponse response = client.execute(fallbackRequest)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                if (statusCode != 200) {
                    log.error("LinkedIn personal read validation failed | transactionId={} | externalUserId={} | status={} | response={}",
                            transactionId, externalUserId, statusCode, responseBody);
                    throw new RuntimeException("LinkedIn read validation failed: HTTP " + statusCode);
                }
            }
        }
    }

    private JSONObject fetchXAccountIdentity(OAuthConsumer consumer) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(ConfigKeys.VERIFY_CREDENTIALS_URL);
            consumer.sign(request);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String jsonResponse = EntityUtils.toString(response.getEntity());
                if (statusCode != 200) {
                    log.error("X verify_credentials returned HTTP {} | response={}", statusCode, jsonResponse);
                    throw new RuntimeException("X verify_credentials failed with HTTP " + statusCode + ": " + jsonResponse);
                }
                return new JSONObject(jsonResponse);
            }
        }
    }

    private boolean containsAllHashtags(String content, List<String> hashtags) {
        if (content == null || hashtags == null) return false;
        return hashtags.stream().allMatch(tag -> content.contains(tag));
    }

    private Post getPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id " + postId));
    }

    @Override
    public TweetResponse postTweetWithUploadedFile(TweetRequest request, UserDetailsImpl userDetails) throws Exception {
        Post post = getPostById(request.getPostId());

//		MultipartFile file = request.getFile();
        String accessToken = request.getAccessToken();
        String accessSecret = request.getAccessSecret();

//		File localFile = mediaStorageService.downloadFile(accessToken, accessSecret);
//		byte[] fileBytes = Files.readAllBytes(localFile.toPath());

        long maxSize = 512L * 1024 * 1024;
//		if (fileBytes.length > maxSize) {
//			throw new RuntimeException("File too large. Max allowed size is 512 MB for videos.");
//		}

        OAuthConsumer consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
        consumer.setTokenWithSecret(accessToken, accessSecret);

//		String mediaId = uploadMedia(localFile, fileBytes, consumer);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost tweetPost = new HttpPost(ConfigKeys.TWEET_POST_URL);
            tweetPost.setHeader("Content-Type", "application/json");

            JSONObject body = new JSONObject();
            body.put("text", post.getContent());
//			body.put("media", new JSONObject().put("media_ids", new JSONArray().put(mediaId)));

            tweetPost.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));
            consumer.sign(tweetPost);

            try (CloseableHttpResponse tweetResp = httpClient.execute(tweetPost)) {
                String rawResponse = EntityUtils.toString(tweetResp.getEntity());
                JSONObject data = new JSONObject(rawResponse).getJSONObject("data");

                return new TweetResponse();
            }
        }
    }

    private String uploadMedia(File file, byte[] fileBytes, OAuthConsumer consumer) throws Exception {
        String fileName = file.getName().toLowerCase();

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            if (fileName.endsWith(".mp4")) {
                // INIT
                HttpPost initPost = new HttpPost(ConfigKeys.MEDIA_UPLOAD_URL);
                List<NameValuePair> initParams = new ArrayList<>();
                initParams.add(new BasicNameValuePair("command", "INIT"));
                initParams.add(new BasicNameValuePair("total_bytes", String.valueOf(fileBytes.length)));
                initParams.add(new BasicNameValuePair("media_type", "video/mp4"));
                initParams.add(new BasicNameValuePair("media_category", "tweet_video"));
                initPost.setEntity(new UrlEncodedFormEntity(initParams, StandardCharsets.UTF_8));
                consumer.sign(initPost);

                String mediaId;
                try (CloseableHttpResponse initResp = httpClient.execute(initPost)) {
                    JSONObject initJson = new JSONObject(EntityUtils.toString(initResp.getEntity()));
                    mediaId = initJson.getString("media_id_string");
                }

                int chunkSize = 4 * 1024 * 1024;
                int segmentIndex = 0;
                for (int offset = 0; offset < fileBytes.length; offset += chunkSize) {
                    int end = Math.min(fileBytes.length, offset + chunkSize);
                    byte[] chunk = Arrays.copyOfRange(fileBytes, offset, end);

                    HttpPost appendPost = new HttpPost(ConfigKeys.MEDIA_UPLOAD_URL);
//					MultipartEntityBuilder builder = MultipartEntityBuilder.create();
//					builder.addTextBody("command", "APPEND");
//					builder.addTextBody("media_id", mediaId);
//					builder.addTextBody("segment_index", String.valueOf(segmentIndex));
//					builder.addBinaryBody("media", chunk, ContentType.DEFAULT_BINARY, file.getName());
//					appendPost.setEntity(builder.build());
                    consumer.sign(appendPost);

                    try (CloseableHttpResponse appendResp = httpClient.execute(appendPost)) {
                        EntityUtils.consume(appendResp.getEntity());
                    }
                    segmentIndex++;
                }

                HttpPost finalizePost = new HttpPost(ConfigKeys.MEDIA_UPLOAD_URL);
                List<NameValuePair> finalizeParams = List.of(
                        new BasicNameValuePair("command", "FINALIZE"),
                        new BasicNameValuePair("media_id", mediaId)
                );
                finalizePost.setEntity(new UrlEncodedFormEntity(finalizeParams, StandardCharsets.UTF_8));
                consumer.sign(finalizePost);

                JSONObject finalizeJson;
                try (CloseableHttpResponse finalizeResp = httpClient.execute(finalizePost)) {
                    finalizeJson = new JSONObject(EntityUtils.toString(finalizeResp.getEntity()));
                }

                if (finalizeJson.has("processing_info")) {
                    JSONObject pInfo = finalizeJson.getJSONObject("processing_info");
                    int checkAfterSecs = pInfo.optInt("check_after_secs", 5);
                    boolean processing = true;

                    while (processing) {
                        Thread.sleep(checkAfterSecs * 1000L);

                        HttpGet statusGet = new HttpGet(
                                ConfigKeys.MEDIA_UPLOAD_URL + "?command=STATUS&media_id=" + mediaId
                        );
                        consumer.sign(statusGet);

                        try (CloseableHttpResponse statusResp = httpClient.execute(statusGet)) {
                            JSONObject statusJson = new JSONObject(EntityUtils.toString(statusResp.getEntity()));
                            if (statusJson.has("processing_info")) {
                                JSONObject proc = statusJson.getJSONObject("processing_info");
                                String state = proc.getString("state");

                                if ("succeeded".equals(state)) {
                                    processing = false;
                                } else if ("failed".equals(state)) {
                                    throw new RuntimeException("Video processing failed: " + statusJson);
                                } else {
                                    checkAfterSecs = proc.optInt("check_after_secs", 5);
                                }
                            } else {
                                processing = false;
                            }
                        }
                    }
                }
                return mediaId;

            } else {
                HttpPost uploadPost = new HttpPost(ConfigKeys.MEDIA_UPLOAD_URL);
//				HttpEntity uploadEntity = MultipartEntityBuilder.create()
//						.addBinaryBody("media", fileBytes, ContentType.DEFAULT_BINARY, file.getName())
//						.build();
//				uploadPost.setEntity(uploadEntity);
                consumer.sign(uploadPost);

                try (CloseableHttpResponse uploadResp = httpClient.execute(uploadPost)) {
                    JSONObject json = new JSONObject(EntityUtils.toString(uploadResp.getEntity()));
                    if (!json.has("media_id_string")) {
                        throw new RuntimeException("Twitter upload failed: " + json);
                    }
                    return json.getString("media_id_string");
                }
            }
        }
    }

    @Override
    public TweetResponse postTweet(TweetRequest request, UserDetailsImpl userDetails) throws Exception {
        Post post = getPostById(request.getPostId());
        List<PostMedia> postMediaList = postMediaRepository.findByPostId(request.getPostId());
        Platform platform = Platform.X;

        Optional<UserAuthToken> userAuthTokenData = userAuthTokenRepository.findByUserIdAndPlatform(userDetails.getUserId(), platform);
        if (userAuthTokenData.isPresent()) {
            UserAuthToken userAuthToken = userAuthTokenData.get();

            if ("BUSINESS".equals(userAuthToken.getOauthSourcePage())) {
                return buildFailureResponse("Cannot post using business page connection. Please connect your personal account.");
            }

            String accessToken = userAuthToken.getAccessToken();
            String accessSecret = userAuthToken.getAccessSecret();
            log.info("accessToken:{},accessSecret:{}",accessToken,accessSecret);
            OAuthConsumer consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
            consumer.setTokenWithSecret(accessToken, accessSecret);

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

                boolean isRetweet = false;
                String content;

                if ("SHARE".equals(request.getType())) {
                    content = request.getContent();
                    try {
                        content = orgBusinessPageContentService.composeBusinessPageContent(
                                userDetails.getOrganization().getId(),
                                platform,
                                content
                        );
                        PlatformBusinessPageComposer.validateLength(content, platform);
                    } catch (IllegalArgumentException ex) {
                        return buildFailureResponse(ex.getMessage());
                    }
                } else {
                    content = StringUtils.hasText(request.getContent())
                            ? request.getContent()
                            : post.getContent();

                    List<String> hashTags = postHashtagRepository.fetchByPostId(request.getPostId());

                    try {
                        content = orgBusinessPageContentService.composeBusinessPageContent(
                                userDetails.getOrganization().getId(),
                                platform,
                                content
                        );
                        if(!hashTags.isEmpty() && !containsAllHashtags(content, hashTags)){
                            content = content + "\n\n" + String.join(", ", hashTags);
                        }
                        PlatformBusinessPageComposer.validateLength(content, platform);
                    } catch (IllegalArgumentException ex) {
                        return buildFailureResponse(ex.getMessage());
                    }
                }

                ObjectNode rootNode = mapper.createObjectNode();
                rootNode.put("text", content);

                if("SHARE".equals(request.getType())){
                    String postExternalId = getPostExternalId(request.getPostId(),post,platform);

                    if(!StringUtils.hasText(postExternalId)){
                        return buildFailureResponse("X share target not found");
                    }

//                        ObjectNode replyNode = mapper.createObjectNode();
//                        replyNode.put("in_reply_to_tweet_id", post.getPlatformUniqueId());
                    rootNode.put("quote_tweet_id", postExternalId);
//                        rootNode.put("tweet_id", post.getPlatformUniqueId());

                    isRetweet = true;
//                        rootNode.set("reply", replyNode);
                }

                log.info("rootNode : {}",rootNode);

                List<String> mediaIdList = new ArrayList<>();

                //Media handling
                //post has images or video
                if("POST".equals(request.getType()) && postMediaList.size() >0) {

                    for (PostMedia postMedia : postMediaList) {
                        if ("IMAGE".equals(postMedia.getMediaType())) {
                            String imageUrn = uploadMediaFromUrlX(postMedia, consumer, postMedia.getMediaType());
                            log.info("image Id X:{}", imageUrn);
                            mediaIdList.add(imageUrn);
                        }else if ("VIDEO".equals(postMedia.getMediaType())) {
                            String imageUrn = uploadVideoFromUrlX(postMedia, consumer);
                            log.info("video Id X:{}", imageUrn);
                            mediaIdList.add(imageUrn);
                        }
                    }
                }

                if(!mediaIdList.isEmpty()) {
                    ObjectNode mediaNode = mapper.createObjectNode();
                    ArrayNode mediaIdsNode = mapper.createArrayNode();
                    for(String mediaId:mediaIdList){
                        mediaIdsNode.add(mediaId);
                    }
                    mediaNode.set("media_ids", mediaIdsNode);
                    rootNode.set("media", mediaNode);
                }


//                HttpPost tweetPost = new HttpPost(isRetweet ? "https://api.twitter.com/2/users/" + userAuthToken.getUserIdExternal() + "/retweets" : ConfigKeys.TWEET_POST_URL);
                HttpPost tweetPost = new HttpPost(ConfigKeys.TWEET_POST_URL);
                tweetPost.setHeader("Content-Type", "application/json");

                tweetPost.setEntity(new StringEntity(rootNode.toString(), StandardCharsets.UTF_8));
                consumer.sign(tweetPost);

                log.info("tweetPost:{}",tweetPost);

                try (CloseableHttpResponse tweetResp = httpClient.execute(tweetPost)) {
                    String rawResponse = EntityUtils.toString(tweetResp.getEntity());
                    log.info("rawResponse:{}",rawResponse);
                    JSONObject responseRoot = new JSONObject(rawResponse);
                    if(responseRoot.has("data")){
                        Long userId = userDetails.getUserId();
                        String userName = userDetails.getUsername();
                        Long tenantId = userDetails.getOrganization().getId();
                        String externalPostId = responseRoot.getJSONObject("data").getString("id");

                        saveExternalShare(request, userId, tenantId, externalPostId, platform,userName);

                        TweetResponse r = new TweetResponse();
                        r.setStatus(true);
                        r.setMessage("Posted successfully");
                        r.setRawResponse(rawResponse);
                        return r;
                    }else{
                        return buildFailureResponse(extractXErrorMessage(responseRoot, rawResponse), rawResponse);
                    }

                }
            }

        } else {

            return buildFailureResponse("X is not connected. Please connect your account in Settings.");
        }
    }

    private TweetResponse buildFailureResponse(String message) {
        return buildFailureResponse(message, null);
    }

    private TweetResponse buildFailureResponse(String message, String rawResponse) {
        TweetResponse response = new TweetResponse();
        response.setStatus(false);
        response.setMessage(message);
        response.setRawResponse(rawResponse);
        return response;
    }

    private String extractXErrorMessage(JSONObject responseRoot, String rawResponse) {
        if (responseRoot.has("errors") && responseRoot.get("errors") instanceof org.json.JSONArray errors && errors.length() > 0) {
            JSONObject error = errors.getJSONObject(0);
            if (error.has("detail")) {
                return error.getString("detail");
            }
            if (error.has("message")) {
                return error.getString("message");
            }
            if (error.has("title")) {
                return error.getString("title");
            }
        }
        return StringUtils.hasText(rawResponse) ? rawResponse : "X post failed";
    }

    private ExternalShare saveExternalShare(TweetRequest request, Long userId, Long tenantId, String externalPostId, Platform platform, String userName) {
        Integer points = null;
        try {
            points = loyaltyPointHelper.handleLoyaltyPoints(request.getPostId(), userId, request.getType(), platform,userName);
        } catch (Exception e) {
            log.error("Loyalty point handling error:{}",e.getMessage());
        }

        ExternalShare externalShare = new ExternalShare();

        externalShare.setPostId(request.getPostId());
        externalShare.setPlatform(platform.toString());
        externalShare.setUserId(userId);
        if("SHARE".equals(request.getType())){
            externalShare.setCaption(request.getContent());
        }
        externalShare.setTenantId(tenantId);
        externalShare.setExternalPostId(externalPostId);
        externalShare.setSharedAt(LocalDateTime.now());
        externalShare.setPoint(points);

        externalShareRepository.save(externalShare);

        return externalShare;
    }

    @Override
    public TweetResponse createLinkedinPost(TweetRequest request, UserDetailsImpl userDetails, User user) throws Exception {
        Post post = getPostById(request.getPostId());
        List<PostMedia> postMediaList = postMediaRepository.findByPostId(request.getPostId());
        Platform platform = Platform.LINKEDIN;

        Long userId = userDetails != null ? userDetails.getUserId():user.getId();

        Optional<UserAuthToken> userAuthTokenData = userAuthTokenRepository.findByUserIdAndPlatform(userId, platform);

        if (userAuthTokenData.isPresent()) {
            UserAuthToken userAuthToken = userAuthTokenData.get();

            if ("BUSINESS".equals(userAuthToken.getOauthSourcePage())) {
                return buildFailureResponse("Cannot post using business page connection. Please connect your personal account.");
            }

            String userIdExternal = userAuthToken.getUserIdExternal();
            String accessToken = userAuthToken.getAccessToken();


            try {
                ObjectNode postRequest = mapper.createObjectNode();

                ObjectNode distribution = mapper.createObjectNode();
                distribution.put("feedDistribution","MAIN_FEED");
                distribution.putArray("targetEntities");
                distribution.putArray("thirdPartyDistributionChannels");

                // Build complete request
                postRequest.put("author", "urn:li:person:" + userIdExternal);
                String content = StringUtils.hasText(request.getContent())
                        ? request.getContent()
                        : post.getContent();

                if(StringUtils.hasText(content)){
                    List<String> hashTags = postHashtagRepository.fetchByPostId(request.getPostId());

                    try {
                        content = orgBusinessPageContentService.composeBusinessPageContent(
                                userDetails.getOrganization().getId(),
                                platform,
                                content
                        );
                    } catch (Exception ex) {
                        log.warn("LinkedIn business page suffix composition failed (non-fatal): {}", ex.getMessage());
                    }

                    if(!hashTags.isEmpty() && !content.contains(String.join(", ", hashTags))){
                        content = content + "\n\n" + String.join(", ", hashTags);
                    }

                    postRequest.put("commentary", content);
                }

                if("SHARE".equals(request.getType())){
                    String postExternalId = getPostExternalId(request.getPostId(),post,platform);

                    // Make it a share, only if the external post id is present
                    if(StringUtils.hasText(postExternalId)){
                        if(StringUtils.hasText(request.getContent())) {
                            postRequest.put("commentary", request.getContent());
                        }

                        ObjectNode reshareContext = mapper.createObjectNode();
                        reshareContext.put("parent", "urn:li:share:" + postExternalId);
                        postRequest.set("reshareContext",reshareContext);
                    }
                }


                postRequest.put("visibility", "PUBLIC");
                postRequest.set("distribution", distribution);
                postRequest.put("lifecycleState", "PUBLISHED");
                postRequest.put("isReshareDisabledByAuthor", false);

                //post has images or video
                if("POST".equals(request.getType()) && postMediaList.size() >0){

                    ObjectNode mediaUploadRequest = mapper.createObjectNode();
                    ObjectNode initializeUploadRequest = mapper.createObjectNode();

                    initializeUploadRequest.put("owner", "urn:li:person:" + userIdExternal);

                    mediaUploadRequest.set("initializeUploadRequest",initializeUploadRequest);


//                    ArrayNode imageUploadRequests = mapper.createArrayNode();
//                    ArrayNode videoUploadRequests = mapper.createArrayNode();
                    List<String> imageUrnList = new ArrayList<>();
                    List<String> videoUrnList = new ArrayList<>();


                    for(PostMedia postMedia:postMediaList){
                        if("IMAGE".equals(postMedia.getMediaType())){
//                            ObjectNode eachImageUploadRequest = mapper.createObjectNode();
//                            eachImageUploadRequest.put("owner", "urn:li:person:" + userIdExternal);
//                            eachImageUploadRequest.put("uploadKey", "" + postMedia.getId());
//                            imageUploadRequests.add(eachImageUploadRequest);

                            String imageUrn = createPostMediaLinkedInPost(mediaUploadRequest,postMediaList, accessToken,postMedia);
                            log.info("image URN:{}",imageUrn);
                            imageUrnList.add(imageUrn);


                        }else if("VIDEO".equals(postMedia.getMediaType())){
                            byte[] videoData = downloadVideoFromUrl(postMedia.getFileUrl());
                            int length = videoData!=null? videoData.length:0;
                            ObjectNode eachVideoUploadRequest = mapper.createObjectNode();
                            eachVideoUploadRequest.put("owner", "urn:li:person:" + userIdExternal);
                            eachVideoUploadRequest.put("fileSizeBytes", length);
                            eachVideoUploadRequest.put("uploadCaptions", false);
                            eachVideoUploadRequest.put("uploadThumbnail", false);

                            ObjectNode videoMediaUploadRequest = mapper.createObjectNode();
                            videoMediaUploadRequest.set("initializeUploadRequest",eachVideoUploadRequest);


                            String videoUrn = createPostVideoLinkedInPost(videoMediaUploadRequest,postMediaList, accessToken,postMedia,videoData);
                            log.info("video URN:{}",videoUrn);
                            videoUrnList.add(videoUrn);
                        }
                    }

//                    if(!imageUploadRequests.isEmpty()){
//                        ObjectNode uploadMultipleImagesRequest = mapper.createObjectNode();
//                        uploadMultipleImagesRequest.set("imageUploadRequests",imageUploadRequests);
//                        initializeUploadRequest.set("uploadMultipleImagesRequest",uploadMultipleImagesRequest);
//                    }
//
//                    if(!videoUploadRequests.isEmpty()){
//                        ObjectNode uploadMultipleVideosRequest = mapper.createObjectNode();
//                        uploadMultipleVideosRequest.set("videoUploadRequests",videoUploadRequests);
//                        initializeUploadRequest.set("uploadMultipleVideosRequest",uploadMultipleVideosRequest);
//                    }


                    if(!imageUrnList.isEmpty() || !videoUrnList.isEmpty()){
                        ObjectNode mediaContent = mapper.createObjectNode();
                        ObjectNode multiImage = mapper.createObjectNode();
                        ObjectNode multiVideo = mapper.createObjectNode();
                        ArrayNode images = mapper.createArrayNode();
                        ArrayNode videos = mapper.createArrayNode();

                        if(!imageUrnList.isEmpty()){
                            for(String imageUrn:imageUrnList){
                                ObjectNode imageNode = mapper.createObjectNode();
                                imageNode.put("id",imageUrn);
                                images.add(imageNode);
                            }
                        }

                        if(!videoUrnList.isEmpty()){
                            for(String imageUrn:videoUrnList){
                                ObjectNode imageNode = mapper.createObjectNode();
                                imageNode.put("id",imageUrn);
                                videos.add(imageNode);
                            }
                        }

                        if(imageUrnList.size()>1){
                            multiImage.set("images",images);
                            mediaContent.set("multiImage",multiImage);
                        }else if(!imageUrnList.isEmpty()){
                            mediaContent.set("media",images.get(0));
                        }

                        if(videoUrnList.size()>1){
                            multiVideo.set("videos",videos);
                            mediaContent.set("multiVideo",multiVideo);
                        }else if(!videoUrnList.isEmpty()) {
                            mediaContent.set("media",videos.get(0));
                        }

                        postRequest.set("content", mediaContent);
                    }
                }

                String fullPostId = createUGCPost(userIdExternal, postRequest,accessToken);

                if(fullPostId != null ) {
                        String userName = null;
                        Long tenantId = null;
                        if(userDetails != null){
                            userName = userDetails.getUsername();
                            tenantId = userDetails.getOrganization().getId();
                        }else {
                            userName = user.getName();
                            tenantId = user.getOrganization().getId();
                        }

                        saveExternalShare(request, userId, tenantId, fullPostId.substring(fullPostId.contains("li")?15:13),Platform.LINKEDIN, userName);

                        TweetResponse r = new TweetResponse();
                        r.setStatus(true);
                        return r;
                }


                return new TweetResponse();

            } catch (Exception e) {
                log.error("Error creating text post for user: {}", userIdExternal, e);
                throw new RuntimeException("Error creating text post", e);
            }



        } else {
            TweetResponse response = new TweetResponse();
            response.setStatus(false);
            response.setMessage("LinkedIn is not connected. Please connect your account in Settings.");
            return response;
        }
    }

    private String createPostMediaLinkedInPost(ObjectNode mediaUploadRequest, List<PostMedia> postMediaList, String token, PostMedia postMedia) {
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;

        try {

            httpPost = new HttpPost("https://api.linkedin.com/rest/images?action=initializeUpload");
            httpPost.setEntity(new StringEntity(mediaUploadRequest.toString(), ContentType.APPLICATION_JSON));
            httpPost.setHeader("Authorization", "Bearer " + token);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("LinkedIn-Version", "202509");
            httpPost.setHeader("X-Restli-Protocol-Version", "2.0.0");

            log.info("Creating media post for Request: {}", mediaUploadRequest.toString());

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                response = httpClient.execute(httpPost);
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                log.info("responseBody:{},statusCode:{}",responseBody,statusCode);
                JsonNode jsonResponse = mapper.readTree(responseBody);

                if (statusCode == 200) {
                    String imageUrn = jsonResponse.path("value").path("image").asText();
                    String imageUploadPath = jsonResponse.path("value").path("uploadUrl").asText();
                    log.info("Image URN:{}, imageUploadPath: {} " , imageUrn, imageUploadPath);

                    uploadMediaLinkedIn(imageUploadPath, postMedia.getFileUrl(),token);

                    return imageUrn;

                } else {
                    throw new RuntimeException("Initialize upload failed: " + responseBody);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
//            log.error("IO error while creating UGC post for user: {}", userId, e);
        } finally {
            closeResources(response, httpPost);
        }
        return null;
    }

    private void uploadMediaLinkedIn(String uploadUrl, String imagePath, String token) throws IOException {
        HttpPut request = new HttpPut(uploadUrl);
        setCommonHeaders(request, token);

        // Remove Content-Type for binary upload, let HttpClient detect it
        request.removeHeaders("Content-Type");

        byte[] imageData = downloadImageFromUrl(imagePath);

        log.info("imageData length:{}",imageData.length);

        String detectedContentType = detectImageContentType(imagePath);
        uploadImageBytes(uploadUrl, imageData, detectedContentType, token);
    }

    private String detectImageContentType(String url) {
        if (url == null) return "image/jpeg";
        String lower = url.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "image/jpeg";
    }

    private void uploadImageBytes(String uploadUrl, byte[] imageData, String contentType, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        try{

        headers.setContentType(MediaType.parseMediaType(contentType));

        // Create the entity with binary data
        org.springframework.http.HttpEntity<byte[]> request = new org.springframework.http.HttpEntity<>(imageData, headers);

        // Make PUT request to the uploadUrl
        ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl, HttpMethod.PUT, request, String.class);

        if (response.getStatusCode() == HttpStatus.CREATED ||
                response.getStatusCode() == HttpStatus.OK) {
            System.out.println("Image uploaded successfully: " + response.getStatusCode());
        } else {
            throw new RuntimeException("Image upload failed: " + response.getStatusCode() +
                    " - " + response.getBody());
        }


        } catch (Exception e) {
            log.error("error:{}",e.getMessage());
        }
    }

    private byte[] downloadImageFromUrl(String imageUrl) {
        // Ensure HTTPS for Bunny CDN URLs
        String resolvedUrl = imageUrl;
        if (resolvedUrl != null && resolvedUrl.startsWith("http://") && resolvedUrl.contains("b-cdn.net")) {
            resolvedUrl = resolvedUrl.replace("http://", "https://");
        }
        ResponseEntity<byte[]> response = restTemplate.exchange(
                resolvedUrl,
                HttpMethod.GET,
                null,
                byte[].class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody();
        } else {
            log.error("Failed to download image | url={} | status={}", resolvedUrl, response.getStatusCode());
            throw new RuntimeException("Failed to download image from URL: " + resolvedUrl + " status=" + response.getStatusCode());
        }
    }

    private void setCommonHeaders(HttpUriRequest request, String token) {
        request.setHeader("Authorization", "Bearer " + token);
        request.setHeader("LinkedIn-Version", "202509");
        request.setHeader("X-Restli-Protocol-Version", "2.0.0");
        request.setHeader("Content-Type", "application/json");
    }

    private String createUGCPost(String userId, JsonNode postRequest,String token) {
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;

        try {

            httpPost = new HttpPost("https://api.linkedin.com/rest/posts");
            httpPost.setEntity(new StringEntity(postRequest.toString(), ContentType.APPLICATION_JSON));
            httpPost.setHeader("Authorization", "Bearer " + token);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("LinkedIn-Version", "202509");
            httpPost.setHeader("X-Restli-Protocol-Version", "2.0.0");

            log.info("Creating UGC post for user: {}, Request: {}", userId, postRequest.toString());



            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                response = httpClient.execute(httpPost);
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
//                log.info("responseBody:{}",responseBody);

                if (statusCode == 201) {
                    String fullPostId = response.getFirstHeader("x-restli-id").getValue();
                    log.info("Successfully created UGC post for user: {}, Post ID: {}",
                            userId, fullPostId);
                    return fullPostId;
                } else {
                    log.error("Failed to create UGC post. Status: {}, Response: {}", statusCode, responseBody);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }




        } catch (Exception e) {
            log.error("IO error while creating UGC post for user: {}", userId, e);
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

    private String getPostExternalId(Long postId, Post post, Platform platform){
        String postExternalId = null;
        if(post.getType()!=null){
            if(post.getType().equals(platform.toString())){
                postExternalId = post.getPlatformUniqueId();
            }else if(post.getType().equals("MANUAL") && post.getCreatedBy() !=null){
                ExternalShare externalShare = externalShareRepository.findByUserIdAndPostIdAndPlatform(post.getCreatedBy().getId(), postId,platform.toString());
                postExternalId = externalShare.getExternalPostId();
            }
        }

        return  postExternalId;
    }

    @Override
    public ImpressionsResponse getImpressionsByPostId(Long postId) {
        try {
            ImpressionsResponse impressionsResponse = initImpressionsResponse();
            Post post = getPostById(postId);
            List<ExternalShare> externalShares = externalShareRepository.findByPostId(postId);
            List<AnalyticsReference> references = buildAnalyticsReferences(post, externalShares);
            Set<Platform> availablePlatforms = new LinkedHashSet<>();
            Set<Platform> unavailablePlatforms = new LinkedHashSet<>();

            // Fetch all platform analytics in parallel with 15s total timeout
            List<java.util.concurrent.CompletableFuture<Void>> futures = references.stream()
                    .map(ref -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                        try {
                            AnalyticsSnapshot snapshot = fetchAnalyticsSnapshot(ref, unavailablePlatforms);
                            if (snapshot != null) {
                                synchronized (impressionsResponse) {
                                    mergePlatformMetrics(impressionsResponse, ref.platform, snapshot);
                                    availablePlatforms.add(ref.platform);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Analytics fetch error for {} {}", ref.platform, ref.externalPostId, e);
                            unavailablePlatforms.add(ref.platform);
                        }
                    }))
                    .toList();
            try {
                java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                        .get(15, java.util.concurrent.TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException te) {
                log.warn("Analytics fetch timed out after 15s, returning partial results");
            } catch (Exception e) {
                log.error("Analytics parallel fetch error", e);
            }

            calculateTotalImpressions(impressionsResponse);
            impressionsResponse.setMessage("Post insights retrieved");
            impressionsResponse.setDevMessage(buildAnalyticsDevMessage(availablePlatforms, unavailablePlatforms));

            return impressionsResponse;
        } catch (Exception e) {
            throw new RuntimeException("Error generating OAuth authorization URL: " + e.getMessage(), e);
        }
    }

    private void calculateTotalImpressions(ImpressionsResponse r) {
        r.setTotalImpressions(sumMap(r.getPlatformImpression()));
        r.setTotalEngagements(sumMap(r.getPlatformEngagement()));
        r.setTotalReach(sumMap(r.getPlatformReach()));
        r.setTotalLikes(sumMap(r.getPlatformLikes()));
        r.setTotalComments(sumMap(r.getPlatformComments()));
        r.setTotalShares(sumMap(r.getPlatformShares()));
        r.setTotalSaves(sumMap(r.getPlatformSaves()));
        r.setTotalBookmarks(sumMap(r.getPlatformBookmarks()));
        r.setTotalVideoViews(sumMap(r.getPlatformVideoViews()));

        int totalClicks = sumMap(r.getPlatformClickCount());
        r.setClickThroughRate(r.getTotalImpressions() > 0
                ? (double) totalClicks / r.getTotalImpressions() * 100
                : 0.0);
    }

    private int sumMap(Map<String, Integer> map) {
        return map.values().stream().mapToInt(Integer::intValue).sum();
    }

    private ImpressionsResponse initImpressionsResponse() {
        ImpressionsResponse response = new ImpressionsResponse();
        response.setStatus(true);

        response.setPlatformImpression(newPlatformMap());
        response.setPlatformEngagement(newPlatformMap());
        response.setPlatformReach(newPlatformMap());
        response.setPlatformClickCount(newPlatformMap());
        response.setPlatformLikes(newPlatformMap());
        response.setPlatformComments(newPlatformMap());
        response.setPlatformShares(newPlatformMap());
        response.setPlatformSaves(newPlatformMap());
        response.setPlatformBookmarks(newPlatformMap());
        response.setPlatformRetweets(newPlatformMap());
        response.setPlatformReplies(newPlatformMap());
        response.setPlatformQuotes(newPlatformMap());
        response.setPlatformVideoViews(newPlatformMap());

        Map<String, Double> platformCtrMap = new LinkedHashMap<>();
        platformCtrMap.put(LINKEDIN_KEY, 0.0);
        platformCtrMap.put(X_KEY, 0.0);
        platformCtrMap.put(FACEBOOK_KEY, 0.0);
        platformCtrMap.put(INSTAGRAM_KEY, 0.0);
        response.setPlatformClickThroughRate(platformCtrMap);

        response.setTotalImpressions(0);
        response.setTotalEngagements(0);
        response.setTotalReach(0);
        response.setClickThroughRate(0.0);
        response.setTotalLikes(0);
        response.setTotalComments(0);
        response.setTotalShares(0);
        response.setTotalSaves(0);
        response.setTotalBookmarks(0);
        response.setTotalVideoViews(0);

        return response;
    }

    private Map<String, Integer> newPlatformMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(LINKEDIN_KEY, 0);
        map.put(X_KEY, 0);
        map.put(FACEBOOK_KEY, 0);
        map.put(INSTAGRAM_KEY, 0);
        return map;
    }

    private AnalyticsSnapshot parseSinglePostStatsForLinkedIn(String responseBody, String expectedPostId) throws Exception {
        JsonNode response = mapper.readTree(responseBody);

        if (response.has("elements") && response.get("elements").size() > 0) {
            JsonNode element = response.get("elements").get(0);

            if (element.has("share") && element.has("totalShareStatistics")) {
                String shareUrn = element.get("share").asText();
                String postId = extractPostIdFromShareUrn(shareUrn);

                log.info("postId:{},expectedPostId:{}",postId,expectedPostId);

                // Verify it's the post we're looking for
                if (postId.equals(expectedPostId)) {
                    JsonNode statistics = element.get("totalShareStatistics");
                    return extractStatsFromJson(statistics);
                }
            }
        }

        return null;
    }

    private AnalyticsSnapshot extractStatsFromJson(JsonNode statistics) {
        Integer impressionCount = 0;
        Integer uniqueImpressionsCount = 0;
        Integer clickCount = 0;
        Double engagementRate = 0.0;
        log.info("linkedin post statistics: {}", statistics);
        if (statistics.has("impressionCount")) {
            impressionCount = statistics.get("impressionCount").asInt();
            log.info("linkedin post statistics in impressionCount: {}", impressionCount);
        }
        if (statistics.has("uniqueImpressionsCount")) {
            uniqueImpressionsCount = statistics.get("uniqueImpressionsCount").asInt();
        }
        if (statistics.has("clickCount")) {
            clickCount = statistics.get("clickCount").asInt();
        }
//        if (statistics.has("likeCount")) {
//            stats.setLikes(statistics.get("likeCount").asInt());
//        }
//        if (statistics.has("commentCount")) {
//            stats.setComments(statistics.get("commentCount").asInt());
//        }
//        if (statistics.has("shareCount")) {
//            stats.setShares(statistics.get("shareCount").asInt());
//        }
        if (statistics.has("engagement")) {
            engagementRate = statistics.get("engagement").asDouble();
        }

        return new AnalyticsSnapshot(
                impressionCount, uniqueImpressionsCount,
                0, 0, 0, 0, 0, 0, 0, 0, clickCount, 0
        );
    }

    private String extractPostIdFromShareUrn(String shareUrn) {
        if (shareUrn.startsWith("urn:li:share:")) {
            return shareUrn.substring("urn:li:share:".length());
        }
        return shareUrn;
    }

    private List<AnalyticsReference> buildAnalyticsReferences(Post post, List<ExternalShare> externalShares) {
        Map<String, AnalyticsReference> uniqueReferences = new LinkedHashMap<>();
        Platform postPlatform = parsePlatform(post.getType());

        if (postPlatform != null && StringUtils.hasText(post.getPlatformUniqueId())) {
            boolean isBusinessPage = "BUSINESS_PAGE".equals(post.getSourceType());
            Long orgId = post.getOrganization() != null ? post.getOrganization().getId() : null;
            addAnalyticsReference(
                    uniqueReferences,
                    new AnalyticsReference(
                            postPlatform,
                            post.getCreatedBy() != null ? post.getCreatedBy().getId() : null,
                            post.getPlatformUniqueId(),
                            post.getPlatformCreatedAt() != null ? post.getPlatformCreatedAt() : post.getCreatedAt(),
                            orgId,
                            isBusinessPage
                    )
            );
        }

        if (externalShares != null) {
            for (ExternalShare externalShare : externalShares) {
                Platform platform = parsePlatform(externalShare.getPlatform());
                if (platform == null || !StringUtils.hasText(externalShare.getExternalPostId())) {
                    continue;
                }

                addAnalyticsReference(
                        uniqueReferences,
                        new AnalyticsReference(
                                platform,
                                externalShare.getUserId(),
                                externalShare.getExternalPostId(),
                                externalShare.getSharedAt()
                        )
                );
            }
        }

        return new ArrayList<>(uniqueReferences.values());
    }

    private void addAnalyticsReference(Map<String, AnalyticsReference> uniqueReferences, AnalyticsReference reference) {
        String key = String.format(
                "%s|%s|%s",
                reference.platform,
                reference.userId != null ? reference.userId : "system",
                reference.externalPostId
        );
        uniqueReferences.putIfAbsent(key, reference);
    }

    Platform parsePlatform(String platformValue) {
        if (!StringUtils.hasText(platformValue)) {
            return null;
        }

        try {
            return Platform.valueOf(platformValue);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    AnalyticsSnapshot fetchAnalyticsSnapshot(AnalyticsReference reference, Set<Platform> unavailablePlatforms) {
        try {
            // For business page posts, try org-level token first
            if (reference.useOrgToken && reference.organizationId != null
                    && (reference.platform == Platform.FACEBOOK || reference.platform == Platform.INSTAGRAM)) {
                Optional<String> orgToken = findOrgToken(reference.organizationId, reference.platform);
                if (orgToken.isPresent()) {
                    return switch (reference.platform) {
                        case FACEBOOK -> fetchFacebookPostAnalyticsWithToken(reference.externalPostId, orgToken.get(), unavailablePlatforms);
                        case INSTAGRAM -> fetchInstagramMediaAnalyticsWithToken(reference.externalPostId, orgToken.get(), unavailablePlatforms);
                        default -> null;
                    };
                }
            }
            return switch (reference.platform) {
                case LINKEDIN -> fetchLinkedInAnalytics(reference.userId, reference.externalPostId, unavailablePlatforms);
                case FACEBOOK -> fetchFacebookPostAnalytics(reference.userId, reference.externalPostId, unavailablePlatforms);
                case INSTAGRAM -> fetchInstagramMediaAnalytics(reference.userId, reference.externalPostId, unavailablePlatforms);
                case X -> fetchXTweetAnalytics(reference.userId, reference.externalPostId, unavailablePlatforms);
            };
        } catch (Exception e) {
            log.error("Error fetching {} analytics for external post {}", reference.platform, reference.externalPostId, e);
            unavailablePlatforms.add(reference.platform);
            return null;
        }
    }

    private Optional<String> findOrgToken(Long organizationId, Platform platform) {
        return externalPlatformRepository.findByOrganizationIdAndPlatformNameIgnoreCase(organizationId, platform.getDisplayName())
                .map(ep -> {
                    try {
                        JsonNode creds = mapper.readTree(ep.getCredentials());
                        JsonNode tokenNode = creds.get("accessToken");
                        return tokenNode != null && !tokenNode.isNull() ? tokenNode.asText() : null;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(StringUtils::hasText);
    }

    private AnalyticsSnapshot fetchFacebookPostAnalyticsWithToken(String externalPostId, String accessToken, Set<Platform> unavailablePlatforms) {
        try {
            int reactions = 0, commentCount = 0, shareCount = 0;
            try {
                JsonNode engagementResponse = fetchMetaNode(
                        String.format("%s/%s?fields=reactions.limit(0).summary(true),comments.limit(0).summary(true),shares&access_token=%s",
                                META_GRAPH_BASE_URL, externalPostId, accessToken));
                reactions = extractSummaryTotal(engagementResponse, "reactions");
                commentCount = extractSummaryTotal(engagementResponse, "comments");
                shareCount = extractNestedInteger(engagementResponse, "shares", "count");
            } catch (Exception engErr) {
                log.warn("FB engagement fetch (org token) failed for {}: {}", externalPostId, engErr.getMessage());
            }

            int impressions = 0, reach = 0, clickCount = 0;
            try {
                JsonNode insightsResponse = fetchFacebookInsights(externalPostId, accessToken);
                if (insightsResponse != null) {
                    impressions = extractMetaInsightInteger(insightsResponse, "post_impressions");
                    reach = extractMetaInsightInteger(insightsResponse, "post_impressions_unique");
                    clickCount = extractFacebookClickCount(insightsResponse, accessToken, externalPostId);
                }
            } catch (Exception insErr) {
                log.debug("FB insights (org token) not available for {}", externalPostId);
            }

            if (reactions == 0 && commentCount == 0 && shareCount == 0 && impressions == 0) {
                return null; // Let it fall through to personal token attempt
            }
            return new AnalyticsSnapshot(impressions, reach, reactions, commentCount, shareCount, 0, 0, 0, 0, 0, clickCount, 0);
        } catch (Exception e) {
            log.error("Error fetching FB analytics with org token for {}", externalPostId, e);
            return null;
        }
    }

    private AnalyticsSnapshot fetchInstagramMediaAnalyticsWithToken(String externalPostId, String accessToken, Set<Platform> unavailablePlatforms) {
        try {
            JsonNode insightsResponse = null;
            try {
                insightsResponse = fetchMetaNode(String.format("%s/%s/insights?metric=impressions,reach,total_interactions&access_token=%s",
                        META_GRAPH_BASE_URL, externalPostId, accessToken));
            } catch (RuntimeException e1) {
                try {
                    insightsResponse = fetchMetaNode(String.format("%s/%s/insights?metric=impressions,reach,likes,comments,shares&access_token=%s",
                            META_GRAPH_BASE_URL, externalPostId, accessToken));
                } catch (RuntimeException e2) {
                    log.warn("IG insights failed with org token for {}", externalPostId);
                }
            }
            JsonNode mediaResponse = fetchMetaNode(String.format("%s/%s?fields=like_count,comments_count&access_token=%s",
                    META_GRAPH_BASE_URL, externalPostId, accessToken));

            int impressions = insightsResponse != null ? extractMetaInsightInteger(insightsResponse, "impressions") : 0;
            int reach = insightsResponse != null ? extractMetaInsightInteger(insightsResponse, "reach") : 0;
            int igLikes = insightsResponse != null ? extractMetaInsightInteger(insightsResponse, "likes") : 0;
            int igComments = insightsResponse != null ? extractMetaInsightInteger(insightsResponse, "comments") : 0;
            int igShares = insightsResponse != null ? extractMetaInsightInteger(insightsResponse, "shares") : 0;
            int igSaved = insightsResponse != null ? extractMetaInsightInteger(insightsResponse, "saved") : 0;
            if (igLikes == 0) igLikes = extractDirectInteger(mediaResponse, "like_count");
            if (igComments == 0) igComments = extractDirectInteger(mediaResponse, "comments_count");

            return new AnalyticsSnapshot(impressions, reach, igLikes, igComments, igShares, igSaved, 0, 0, 0, 0, 0, 0);
        } catch (Exception e) {
            log.error("Error fetching IG analytics with org token for {}", externalPostId, e);
            unavailablePlatforms.add(Platform.INSTAGRAM);
            return null;
        }
    }

    private AnalyticsSnapshot fetchLinkedInAnalytics(Long userId, String externalPostId, Set<Platform> unavailablePlatforms) {
        if (!StringUtils.hasText(externalPostId)) {
            unavailablePlatforms.add(Platform.LINKEDIN);
            return null;
        }

        Optional<UserAuthToken> tokenData = findTokenForUser(userId, Platform.LINKEDIN);
        if (tokenData.isEmpty()) {
            unavailablePlatforms.add(Platform.LINKEDIN);
            return null;
        }

        String accessToken = tokenData.get().getAccessToken();

        try {
            // Try socialActions endpoint first (works with w_member_social scope)
            String url = String.format(
                    "https://api.linkedin.com/v2/socialActions/urn:li:share:%s",
                    externalPostId
            );

            HttpResponse<String> response = executeJsonGet(url, Map.of(
                    "Authorization", "Bearer " + accessToken
            ));

            if (response.statusCode() != 200) {
                log.warn("LinkedIn socialActions fetch failed for {} with status {} — trying UGC format", externalPostId, response.statusCode());
                // Retry with UGC activity URN format
                url = String.format(
                        "https://api.linkedin.com/v2/socialActions/urn:li:activity:%s",
                        externalPostId
                );
                response = executeJsonGet(url, Map.of(
                        "Authorization", "Bearer " + accessToken
                ));
            }

            if (response.statusCode() != 200) {
                log.error("LinkedIn analytics fetch failed for {} with status {} and body {}", externalPostId, response.statusCode(), response.body());
                unavailablePlatforms.add(Platform.LINKEDIN);
                return null;
            }

            // Parse socialActions response: { likesSummary: { totalLikes }, commentsSummary: { totalFirstLevelComments }, ... }
            JSONObject json = new JSONObject(response.body());
            int likes = 0;
            int comments = 0;
            if (json.has("likesSummary")) {
                likes = json.getJSONObject("likesSummary").optInt("totalLikes", 0);
            }
            if (json.has("commentsSummary")) {
                comments = json.getJSONObject("commentsSummary").optInt("totalFirstLevelComments", 0);
            }
            // LinkedIn socialActions doesn't provide impressions/reach
            return new AnalyticsSnapshot(0, 0, likes, comments, 0, 0, 0, 0, 0, 0, 0, 0);
        } catch (Exception e) {
            log.error("Error in getting analytics for linkedin post {}", externalPostId, e);
            unavailablePlatforms.add(Platform.LINKEDIN);
            return null;
        }
    }

    private AnalyticsSnapshot fetchFacebookPostAnalytics(Long userId, String externalPostId, Set<Platform> unavailablePlatforms) {
        Optional<UserAuthToken> userAuthTokenData = findTokenForUser(userId, Platform.FACEBOOK);
        if (userAuthTokenData.isEmpty()) {
            unavailablePlatforms.add(Platform.FACEBOOK);
            return null;
        }

        String accessToken = userAuthTokenData.get().getAccessToken();

        try {
            // Always fetch engagement data first (works without pages_read_engagement)
            int reactions = 0, commentCount = 0, shareCount = 0;
            try {
                JsonNode engagementResponse = fetchMetaNode(
                        String.format("%s/%s?fields=reactions.limit(0).summary(true),comments.limit(0).summary(true),shares&access_token=%s",
                                META_GRAPH_BASE_URL, externalPostId, accessToken));
                reactions = extractSummaryTotal(engagementResponse, "reactions");
                commentCount = extractSummaryTotal(engagementResponse, "comments");
                shareCount = extractNestedInteger(engagementResponse, "shares", "count");
            } catch (Exception engErr) {
                log.warn("FB engagement fetch failed for {}: {}", externalPostId, engErr.getMessage());
            }

            // Try insights (requires pages_read_engagement — may fail gracefully)
            int impressions = 0, reach = 0, clickCount = 0;
            try {
                JsonNode insightsResponse = fetchFacebookInsights(externalPostId, accessToken);
                if (insightsResponse != null) {
                    impressions = extractMetaInsightInteger(insightsResponse, "post_impressions");
                    reach = extractMetaInsightInteger(insightsResponse, "post_impressions_unique");
                    clickCount = extractFacebookClickCount(insightsResponse, accessToken, externalPostId);
                }
            } catch (Exception insErr) {
                log.debug("FB insights not available for {} (missing pages_read_engagement scope)", externalPostId);
            }

            // Return whatever we have — engagement always, insights when available
            if (reactions == 0 && commentCount == 0 && shareCount == 0 && impressions == 0) {
                unavailablePlatforms.add(Platform.FACEBOOK);
                return null;
            }
            return new AnalyticsSnapshot(impressions, reach, reactions, commentCount, shareCount, 0, 0, 0, 0, 0, clickCount, 0);
        } catch (Exception e) {
            log.error("Error in getting analytics for facebook post {}", externalPostId, e);
            unavailablePlatforms.add(Platform.FACEBOOK);
            return null;
        }
    }

    private JsonNode fetchFacebookInsights(String externalPostId, String accessToken) throws Exception {
        try {
            return fetchMetaNode(
                    String.format(
                            "%s/%s/insights?metric=post_impressions,post_impressions_unique,post_clicks&access_token=%s",
                            META_GRAPH_BASE_URL,
                            externalPostId,
                            accessToken
                    )
            );
        } catch (RuntimeException primaryFailure) {
            log.warn("Primary Facebook insights metric set failed for {}. Trying v2 metrics.", externalPostId);
            try {
                return fetchMetaNode(
                        String.format(
                                "%s/%s/insights?metric=post_impressions_unique&access_token=%s",
                                META_GRAPH_BASE_URL,
                                externalPostId,
                                accessToken
                        )
                );
            } catch (RuntimeException secondFailure) {
                log.warn("Facebook insights v2 also failed for {}. Returning null insights.", externalPostId);
                return null;
            }
        }
    }

    private int extractFacebookClickCount(JsonNode insightsResponse, String accessToken, String externalPostId) throws Exception {
        int directClicks = extractMetaInsightInteger(insightsResponse, "post_clicks");
        if (directClicks > 0) {
            return directClicks;
        }

        int clickBreakdown = extractMetaInsightMapTotal(insightsResponse, "post_clicks_by_type_unique");
        if (clickBreakdown > 0) {
            return clickBreakdown;
        }

        JsonNode fallbackInsights = fetchMetaNode(
                String.format(
                        "%s/%s/insights?metric=post_clicks_by_type_unique&access_token=%s",
                        META_GRAPH_BASE_URL,
                        externalPostId,
                        accessToken
                )
        );

        return extractMetaInsightMapTotal(fallbackInsights, "post_clicks_by_type_unique");
    }

    private AnalyticsSnapshot fetchInstagramMediaAnalytics(Long userId, String externalPostId, Set<Platform> unavailablePlatforms) {
        Optional<UserAuthToken> userAuthTokenData = findTokenForUser(userId, Platform.INSTAGRAM);
        if (userAuthTokenData.isEmpty()) {
            unavailablePlatforms.add(Platform.INSTAGRAM);
            return null;
        }

        String accessToken = userAuthTokenData.get().getAccessToken();

        try {
            // Always try media endpoint first (like_count, comments_count — always works for own media)
            int igLikes = 0, igComments = 0;
            try {
                JsonNode mediaResponse = fetchMetaNode(
                        String.format("%s/%s?fields=like_count,comments_count&access_token=%s",
                                META_GRAPH_BASE_URL, externalPostId, accessToken));
                igLikes = extractDirectInteger(mediaResponse, "like_count");
                igComments = extractDirectInteger(mediaResponse, "comments_count");
            } catch (Exception mediaErr) {
                log.debug("IG media fields not available for {}: {}", externalPostId, mediaErr.getMessage());
            }

            // Try insights (requires instagram_manage_insights — may fail gracefully)
            int impressions = 0, reach = 0, igShares = 0, igSaved = 0;
            try {
                JsonNode insightsResponse = fetchMetaNode(
                        String.format("%s/%s/insights?metric=impressions,reach,total_interactions&access_token=%s",
                                META_GRAPH_BASE_URL, externalPostId, accessToken));
                impressions = extractMetaInsightInteger(insightsResponse, "impressions");
                reach = extractMetaInsightInteger(insightsResponse, "reach");
                int totalInteractions = extractMetaInsightInteger(insightsResponse, "total_interactions");
                if (igLikes == 0 && totalInteractions > 0) igLikes = totalInteractions;
            } catch (Exception insErr) {
                log.debug("IG insights not available for {} (trying individual metrics)", externalPostId);
                try {
                    JsonNode insightsResponse = fetchMetaNode(
                            String.format("%s/%s/insights?metric=impressions,reach,likes,comments,shares,saved&access_token=%s",
                                    META_GRAPH_BASE_URL, externalPostId, accessToken));
                    impressions = extractMetaInsightInteger(insightsResponse, "impressions");
                    reach = extractMetaInsightInteger(insightsResponse, "reach");
                    int insLikes = extractMetaInsightInteger(insightsResponse, "likes");
                    int insComments = extractMetaInsightInteger(insightsResponse, "comments");
                    igShares = extractMetaInsightInteger(insightsResponse, "shares");
                    igSaved = extractMetaInsightInteger(insightsResponse, "saved");
                    if (insLikes > igLikes) igLikes = insLikes;
                    if (insComments > igComments) igComments = insComments;
                } catch (Exception fallbackErr) {
                    log.debug("IG insights fully unavailable for {}", externalPostId);
                }
            }

            // Return whatever we have
            if (igLikes == 0 && igComments == 0 && impressions == 0) {
                unavailablePlatforms.add(Platform.INSTAGRAM);
                return null;
            }
            return new AnalyticsSnapshot(impressions, reach, igLikes, igComments, igShares, igSaved, 0, 0, 0, 0, 0, 0);
        } catch (Exception e) {
            log.error("Error in getting analytics for instagram media {}", externalPostId, e);
            unavailablePlatforms.add(Platform.INSTAGRAM);
            return null;
        }
    }

    private AnalyticsSnapshot fetchXTweetAnalytics(Long userId, String externalPostId, Set<Platform> unavailablePlatforms) {
        Optional<UserAuthToken> tokenData = findTokenForUser(userId, Platform.X);
        if (tokenData.isEmpty()) {
            unavailablePlatforms.add(Platform.X);
            return null;
        }

        try {
            String url = "https://api.twitter.com/2/tweets/" + externalPostId + "?tweet.fields=public_metrics";

            OAuthConsumer consumer = new CommonsHttpOAuthConsumer(consumerKey, consumerSecret);
            consumer.setTokenWithSecret(tokenData.get().getAccessToken(), tokenData.get().getAccessSecret());

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .header("Content-Type", "application/json");

            // Sign using OAuth 1.0a — build a temporary signed Apache request to extract the Authorization header
            org.apache.http.client.methods.HttpGet tempRequest = new org.apache.http.client.methods.HttpGet(url);
            consumer.sign(tempRequest);
            String authHeader = tempRequest.getFirstHeader("Authorization").getValue();
            requestBuilder.header("Authorization", authHeader);

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 402) {
                log.warn("X API credits depleted — cannot fetch analytics for tweet {}", externalPostId);
                unavailablePlatforms.add(Platform.X);
                return null;
            }

            if (response.statusCode() != 200) {
                log.error("X analytics fetch failed for {} with status {} and body {}", externalPostId, response.statusCode(), response.body());
                unavailablePlatforms.add(Platform.X);
                return null;
            }

            JSONObject json = new JSONObject(response.body());
            JSONObject metrics = json.optJSONObject("data");
            if (metrics == null) {
                unavailablePlatforms.add(Platform.X);
                return null;
            }
            JSONObject publicMetrics = metrics.optJSONObject("public_metrics");
            if (publicMetrics == null) {
                unavailablePlatforms.add(Platform.X);
                return null;
            }

            int impressions = publicMetrics.optInt("impression_count", 0);
            int likes = publicMetrics.optInt("like_count", 0);
            int retweets = publicMetrics.optInt("retweet_count", 0);
            int replies = publicMetrics.optInt("reply_count", 0);
            int quotes = publicMetrics.optInt("quote_count", 0);
            int bookmarks = publicMetrics.optInt("bookmark_count", 0);

            return new AnalyticsSnapshot(impressions, 0, likes, 0, 0, 0, bookmarks, retweets, replies, quotes, 0, 0);
        } catch (Exception e) {
            log.error("Error in getting analytics for X tweet {}", externalPostId, e);
            unavailablePlatforms.add(Platform.X);
            return null;
        }
    }

    private Optional<UserAuthToken> findTokenForUser(Long userId, Platform platform) {
        if (userId == null) {
            log.warn("Cannot fetch analytics: userId is null for platform {}", platform);
            return Optional.empty();
        }

        Optional<UserAuthToken> token = userAuthTokenRepository.findByUserIdAndPlatform(userId, platform)
                .filter(t -> StringUtils.hasText(t.getAccessToken()));
        if (token.isEmpty()) {
            log.warn("No valid {} token found for userId={}", platform, userId);
        }
        return token;
    }

    private HttpResponse<String> executeJsonGet(String url, Map<String, String> headers) throws Exception {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();

        headers.forEach(requestBuilder::header);

        return HttpClient.newHttpClient().send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode fetchMetaNode(String url) throws Exception {
        HttpResponse<String> response = executeJsonGet(url, Collections.emptyMap());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Meta analytics fetch failed with status " + response.statusCode() + " and body " + response.body());
        }

        return mapper.readTree(response.body());
    }

    private int extractMetaInsightInteger(JsonNode response, String metricName) {
        JsonNode insightNode = findInsightMetric(response, metricName);
        if (insightNode == null || !insightNode.has("values") || insightNode.get("values").isEmpty()) {
            return 0;
        }

        JsonNode valueNode = insightNode.get("values").get(0).get("value");
        return valueNode != null && valueNode.isNumber() ? valueNode.asInt() : 0;
    }

    private int extractMetaInsightMapTotal(JsonNode response, String metricName) {
        JsonNode insightNode = findInsightMetric(response, metricName);
        if (insightNode == null || !insightNode.has("values") || insightNode.get("values").isEmpty()) {
            return 0;
        }

        JsonNode valueNode = insightNode.get("values").get(0).get("value");
        if (valueNode == null || !valueNode.isObject()) {
            return 0;
        }

        int total = 0;
        Iterator<JsonNode> iterator = valueNode.elements();
        while (iterator.hasNext()) {
            JsonNode metricValue = iterator.next();
            if (metricValue.isNumber()) {
                total += metricValue.asInt();
            }
        }
        return total;
    }

    private JsonNode findInsightMetric(JsonNode response, String metricName) {
        if (response == null || !response.has("data") || !response.get("data").isArray()) {
            return null;
        }

        for (JsonNode metricNode : response.get("data")) {
            if (metricNode.has("name") && metricName.equals(metricNode.get("name").asText())) {
                return metricNode;
            }
        }

        return null;
    }

    private int extractSummaryTotal(JsonNode response, String fieldName) {
        return extractNestedInteger(response, fieldName, "summary", "total_count");
    }

    private int extractDirectInteger(JsonNode response, String fieldName) {
        JsonNode fieldNode = response != null ? response.get(fieldName) : null;
        return fieldNode != null && fieldNode.isNumber() ? fieldNode.asInt() : 0;
    }

    private int extractNestedInteger(JsonNode response, String... path) {
        JsonNode current = response;
        for (String pathNode : path) {
            if (current == null || !current.has(pathNode)) {
                return 0;
            }
            current = current.get(pathNode);
        }

        return current != null && current.isNumber() ? current.asInt() : 0;
    }

    private void mergePlatformMetrics(ImpressionsResponse response, Platform platform, AnalyticsSnapshot snapshot) {
        String platformKey = platform.toString();
        response.getPlatformImpression().merge(platformKey, snapshot.impressions, Integer::sum);
        response.getPlatformEngagement().merge(platformKey, snapshot.engagements, Integer::sum);
        response.getPlatformReach().merge(platformKey, snapshot.reach, Integer::sum);
        response.getPlatformClickCount().merge(platformKey, snapshot.clicks, Integer::sum);

        // Engagement breakdowns
        response.getPlatformLikes().merge(platformKey, snapshot.likes, Integer::sum);
        response.getPlatformComments().merge(platformKey, snapshot.comments, Integer::sum);
        response.getPlatformShares().merge(platformKey, snapshot.shares, Integer::sum);
        response.getPlatformSaves().merge(platformKey, snapshot.saves, Integer::sum);
        response.getPlatformBookmarks().merge(platformKey, snapshot.bookmarks, Integer::sum);
        response.getPlatformRetweets().merge(platformKey, snapshot.retweets, Integer::sum);
        response.getPlatformReplies().merge(platformKey, snapshot.replies, Integer::sum);
        response.getPlatformQuotes().merge(platformKey, snapshot.quotes, Integer::sum);
        response.getPlatformVideoViews().merge(platformKey, snapshot.videoViews, Integer::sum);

        int updatedImpressions = response.getPlatformImpression().get(platformKey);
        int updatedClicks = response.getPlatformClickCount().get(platformKey);
        response.getPlatformClickThroughRate().put(
                platformKey,
                updatedImpressions > 0 ? (double) updatedClicks / updatedImpressions * 100 : 0.0
        );
    }

    private String buildAnalyticsDevMessage(Set<Platform> availablePlatforms, Set<Platform> unavailablePlatforms) {
        String available = availablePlatforms.isEmpty()
                ? "none"
                : availablePlatforms.stream().map(Enum::name).sorted().reduce((left, right) -> left + ", " + right).orElse("none");
        String unavailable = unavailablePlatforms.isEmpty()
                ? "none"
                : unavailablePlatforms.stream().map(Enum::name).sorted().reduce((left, right) -> left + ", " + right).orElse("none");

        return String.format("Analytics available for: %s. Analytics unavailable for: %s.", available, unavailable);
    }

    private String createPostVideoLinkedInPost(ObjectNode mediaUploadRequest, List<PostMedia> postMediaList, String token, PostMedia postMedia, byte[] videoData) {
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;

        String videoUrn = null;

        try {

            httpPost = new HttpPost("https://api.linkedin.com/rest/videos?action=initializeUpload");
            httpPost.setEntity(new StringEntity(mediaUploadRequest.toString(), ContentType.APPLICATION_JSON));
            httpPost.setHeader("Authorization", "Bearer " + token);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("LinkedIn-Version", "202509");
            httpPost.setHeader("X-Restli-Protocol-Version", "2.0.0");

            log.info("Creating media post for Request: {}", mediaUploadRequest.toString());

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                response = httpClient.execute(httpPost);
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                log.info("responseBody:{},statusCode:{}",responseBody,statusCode);
                JsonNode jsonResponse = mapper.readTree(responseBody);

                if (statusCode == 200) {
                    videoUrn = jsonResponse.path("value").path("video").asText();

                    JsonNode uploadInstructions = jsonResponse.path("value").get("uploadInstructions");

                    String videoUploadPath=null;

                    if (uploadInstructions != null && uploadInstructions.isArray() && !uploadInstructions.isEmpty()) {
                        JsonNode firstElement = uploadInstructions.get(0);
                        videoUploadPath = firstElement.path("uploadUrl").asText();
                    }

                    log.info("Image URN:{}, imageUploadPath: {} " , videoUrn, videoUploadPath);

                    List<UploadPart> uploadIds = uploadVideoLinkedIn(videoUploadPath, postMedia.getFileUrl(),token,videoData);

                    log.info("uploadIds:{}",uploadIds);
                    linkedinFinishUpload(uploadIds,videoUrn,token);

                } else {
                    throw new RuntimeException("Initialize upload failed: " + responseBody);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
//            log.error("IO error while creating UGC post for user: {}", userId, e);
        } finally {
            closeResources(response, httpPost);
        }
        return videoUrn;
    }

    private void linkedinFinishUpload(List<UploadPart> uploadIds, String videoUrn, String token) {
        HttpPost httpPost = null;
        CloseableHttpResponse response = null;

        try {

            JsonNode request = linkedinHelper.createUploadFinalizeRequest(uploadIds, videoUrn);

            httpPost = new HttpPost("https://api.linkedin.com/rest/videos?action=finalizeUpload");
            httpPost.setEntity(new StringEntity(request.toString(), ContentType.APPLICATION_JSON));
            httpPost.setHeader("Authorization", "Bearer " + token);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("LinkedIn-Version", "202509");
            httpPost.setHeader("X-Restli-Protocol-Version", "2.0.0");

            log.info("Creating media post for Request: {}", request.toString());

            try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
                response = httpClient.execute(httpPost);
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                log.info("responseBody:{},statusCode:{}",responseBody,statusCode);
                JsonNode jsonResponse = mapper.readTree(responseBody);

                if (statusCode == 200) {
                    log.info("Image URN:{}, imageUploadFinalized ");
                } else {
                    throw new RuntimeException("Initialize upload failed: " + responseBody);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } catch (Exception e) {
//            log.error("IO error while creating UGC post for user: {}", userId, e);
        } finally {
            closeResources(response, httpPost);
        }
    }

    private List<UploadPart> uploadVideoLinkedIn(String uploadUrl, String imagePath, String token, byte[] videoData) throws IOException {

        log.info("videoData length:{}",videoData.length);

        List<UploadPart> uploadedParts = linkedinHelper.uploadVideoInChunks(uploadUrl, videoData, "test.mp4", token);


        return uploadedParts;


//        MediaType contentType = detectVideoContentType(videoData);
//
//        log.info("Uploading video with content type: {}, uploadUrl: {}" , contentType, uploadUrl);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.set("Authorization", "Bearer " + token);
//        headers.setContentType(contentType);
//
//        List<String> uploadedIds = new ArrayList<>();
//
//        org.springframework.http.HttpEntity<byte[]> request = new org.springframework.http.HttpEntity<>(videoData, headers);
//
//        try {
//
//            ResponseEntity<String> response = restTemplate.exchange(
//                    uploadUrl, HttpMethod.PUT, request, String.class);
//
//            log.info("upload response: {}",response);
//
//            if (response.getStatusCode() == HttpStatus.CREATED ||
//                    response.getStatusCode() == HttpStatus.OK) {
//                log.info("Video uploaded successfully: {}", response.getStatusCode());
//
//                HttpHeaders responseHeaders = response.getHeaders();
//
//                String uploadId = responseHeaders.getFirst("ETag");
//                uploadedIds.add(uploadId);
//            } else {
//                log.error("Video upload failed: {} - {}", response.getStatusCode(), response.getBody());
//            }
//        } catch (Exception e) {
//            log.error("upload error: {}",e.getMessage());
//        }
//
//        return uploadedIds;
    }

    private MediaType detectVideoContentType(byte[] videoData) {
        // Check file signatures (magic numbers)
        if (isMP4(videoData)) {
            return MediaType.parseMediaType("video/mp4");
        } else if (isMOV(videoData)) {
            return MediaType.parseMediaType("video/quicktime");
        } else if (isAVI(videoData)) {
            return MediaType.parseMediaType("video/x-msvideo");
        } else {
            // Default to MP4
            return MediaType.parseMediaType("video/mp4");
        }
    }

    private boolean isMP4(byte[] data) {
        // MP4 signature: ftyp
        return data.length > 8 &&
                data[4] == 'f' && data[5] == 't' && data[6] == 'y' && data[7] == 'p';
    }

    private boolean isMOV(byte[] data) {
        // MOV signature: ftypqt
        return data.length > 12 &&
                data[4] == 'f' && data[5] == 't' && data[6] == 'y' && data[7] == 'p' &&
                data[8] == 'q' && data[9] == 't';
    }

    private boolean isAVI(byte[] data) {
        // AVI signature: RIFF
        return data.length > 4 &&
                data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F';
    }

    private byte[] downloadVideoFromUrl(String videoUrl) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (compatible)");

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                videoUrl, HttpMethod.GET, entity, byte[].class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            long sizeMB = response.getBody().length / (1024 * 1024);
            log.info("Downloaded video size: {} MB" , sizeMB );

            // Check video size limit (LinkedIn max is 5GB)
            if (response.getBody().length > 5L * 1024 * 1024 * 1024) {
                log.error("Video size exceeds LinkedIn's 5GB limit");
            }

            return response.getBody();
        } else {
            log.error("Failed to download video from URL: {}" , response.getStatusCode());
        }
        return null;
    }

    @Override
    public DashboardResponse fetchOrganizationMonthlyReach(String traceId, String tenantId, UserDetailsImpl userDetails) {
        DashboardResponse dashboardResponse = new DashboardResponse();
        dashboardResponse.setMessage("Organization analytics retrieved");

        List<ExternalShare> externalShares = externalShareRepository.findByTenantId(userDetails.getOrganization().getId());
        Map<YearMonth, Integer> monthlyReachByMonth = new java.util.concurrent.ConcurrentHashMap<>();
        Set<Platform> availablePlatforms = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Set<Platform> unavailablePlatforms = java.util.concurrent.ConcurrentHashMap.newKeySet();

        // Deduplicate by external post ID
        Map<String, ExternalShare> uniqueShares = new LinkedHashMap<>();
        for (ExternalShare es : externalShares) {
            if (es.getExternalPostId() != null && es.getSharedAt() != null) {
                uniqueShares.putIfAbsent(es.getPlatform() + "|" + es.getExternalPostId(), es);
            }
        }

        // Fetch analytics in parallel with 20s timeout
        List<java.util.concurrent.CompletableFuture<Void>> futures = uniqueShares.values().stream()
                .map(es -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                    Platform platform = parsePlatform(es.getPlatform());
                    if (platform == null) return;
                    try {
                        boolean useOrgToken = (platform == Platform.FACEBOOK || platform == Platform.INSTAGRAM);
                        AnalyticsSnapshot snapshot = fetchAnalyticsSnapshot(
                                new AnalyticsReference(platform, es.getUserId(), es.getExternalPostId(), es.getSharedAt(), es.getTenantId(), useOrgToken),
                                unavailablePlatforms);
                        if (snapshot != null) {
                            // Use impressions as primary metric (available from X, FB, IG); fall back to reach if impressions=0
                            int value = snapshot.impressions > 0 ? snapshot.impressions : snapshot.reach;
                            if (value > 0) {
                                monthlyReachByMonth.merge(YearMonth.from(es.getSharedAt()), value, Integer::sum);
                            }
                            availablePlatforms.add(platform);
                        }
                    } catch (Exception e) {
                        unavailablePlatforms.add(platform);
                    }
                }))
                .toList();
        try {
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .get(20, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException te) {
            log.warn("Monthly reach analytics timed out after 20s, returning partial results");
        } catch (Exception e) {
            log.error("Monthly reach parallel fetch error", e);
        }

        dashboardResponse.setMonthlyReachCount(buildMonthlyReachResponse(new TreeMap<>(monthlyReachByMonth)));
        dashboardResponse.setDevMessage(buildAnalyticsDevMessage(availablePlatforms, unavailablePlatforms));
        return dashboardResponse;
    }

    @Override
    public DashboardResponse fetchOrganizationLeadConversion(String traceId, String tenantId, UserDetailsImpl userDetails) {
        DashboardResponse dashboardResponse = new DashboardResponse();
        dashboardResponse.setMessage("Organization analytics retrieved");

        List<ExternalShare> externalShares = externalShareRepository.findByTenantId(userDetails.getOrganization().getId());
        Map<Platform, java.util.concurrent.atomic.AtomicInteger> impressionTotals = new java.util.concurrent.ConcurrentHashMap<>();
        Map<Platform, java.util.concurrent.atomic.AtomicInteger> clickTotals = new java.util.concurrent.ConcurrentHashMap<>();
        for (Platform p : Platform.values()) {
            impressionTotals.put(p, new java.util.concurrent.atomic.AtomicInteger(0));
            clickTotals.put(p, new java.util.concurrent.atomic.AtomicInteger(0));
        }
        Set<Platform> availablePlatforms = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Set<Platform> unavailablePlatforms = java.util.concurrent.ConcurrentHashMap.newKeySet();

        // Deduplicate
        Map<String, ExternalShare> uniqueShares = new LinkedHashMap<>();
        for (ExternalShare es : externalShares) {
            if (es.getExternalPostId() != null) {
                uniqueShares.putIfAbsent(es.getPlatform() + "|" + es.getExternalPostId(), es);
            }
        }

        // Parallel with 20s timeout
        List<java.util.concurrent.CompletableFuture<Void>> futures = uniqueShares.values().stream()
                .map(es -> java.util.concurrent.CompletableFuture.runAsync(() -> {
                    Platform platform = parsePlatform(es.getPlatform());
                    if (platform == null) return;
                    try {
                        boolean useOrgToken = (platform == Platform.FACEBOOK || platform == Platform.INSTAGRAM);
                        AnalyticsSnapshot snapshot = fetchAnalyticsSnapshot(
                                new AnalyticsReference(platform, es.getUserId(), es.getExternalPostId(), es.getSharedAt(), es.getTenantId(), useOrgToken),
                                unavailablePlatforms);
                        if (snapshot != null) {
                            impressionTotals.get(platform).addAndGet(snapshot.impressions);
                            clickTotals.get(platform).addAndGet(snapshot.clicks);
                            availablePlatforms.add(platform);
                        }
                    } catch (Exception e) {
                        unavailablePlatforms.add(platform);
                    }
                }))
                .toList();
        try {
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                    .get(20, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException te) {
            log.warn("Lead conversion analytics timed out after 20s, returning partial results");
        } catch (Exception e) {
            log.error("Lead conversion parallel fetch error", e);
        }

        dashboardResponse.setLinkedinLead(buildLeadConversionDto(impressionTotals.get(Platform.LINKEDIN).get(), clickTotals.get(Platform.LINKEDIN).get()));
        dashboardResponse.setFacebookLead(buildLeadConversionDto(impressionTotals.get(Platform.FACEBOOK).get(), clickTotals.get(Platform.FACEBOOK).get()));
        dashboardResponse.setInstagramLead(buildLeadConversionDto(impressionTotals.get(Platform.INSTAGRAM).get(), clickTotals.get(Platform.INSTAGRAM).get()));
        dashboardResponse.setXLead(buildLeadConversionDto(impressionTotals.get(Platform.X).get(), clickTotals.get(Platform.X).get()));
        dashboardResponse.setDevMessage(buildAnalyticsDevMessage(availablePlatforms, unavailablePlatforms));
        return dashboardResponse;
    }

    private AnalyticsSnapshot fetchCachedAnalytics(
            ExternalShare externalShare,
            Map<String, AnalyticsSnapshot> analyticsCache,
            Set<Platform> availablePlatforms,
            Set<Platform> unavailablePlatforms
    ) {
        Platform platform = parsePlatform(externalShare.getPlatform());
        if (platform == null || !StringUtils.hasText(externalShare.getExternalPostId())) {
            return null;
        }

        String cacheKey = platform + "|" + externalShare.getUserId() + "|" + externalShare.getExternalPostId();
        if (analyticsCache.containsKey(cacheKey)) {
            AnalyticsSnapshot cachedSnapshot = analyticsCache.get(cacheKey);
            if (cachedSnapshot != null) {
                availablePlatforms.add(platform);
            }
            return cachedSnapshot;
        }

        // Use org token for Facebook/Instagram — page tokens required for insights
        boolean useOrgToken = (platform == Platform.FACEBOOK || platform == Platform.INSTAGRAM);
        AnalyticsSnapshot snapshot = fetchAnalyticsSnapshot(
                new AnalyticsReference(platform, externalShare.getUserId(), externalShare.getExternalPostId(),
                        externalShare.getSharedAt(), externalShare.getTenantId(), useOrgToken),
                unavailablePlatforms
        );

        analyticsCache.put(cacheKey, snapshot);
        if (snapshot != null) {
            availablePlatforms.add(platform);
        }
        return snapshot;
    }

    private List<MonthlyParticipationDTO> buildMonthlyReachResponse(Map<YearMonth, Integer> monthlyReachByMonth) {
        List<MonthlyParticipationDTO> monthlyParticipationDTOs = new ArrayList<>();
        for (Map.Entry<YearMonth, Integer> entry : monthlyReachByMonth.entrySet()) {
            MonthlyParticipationDTO dto = new MonthlyParticipationDTO();
            dto.setYear(String.valueOf(entry.getKey().getYear()));
            dto.setMonthNumber(String.format("%02d", entry.getKey().getMonthValue()));
            dto.setMonthName(entry.getKey().format(DateTimeFormatter.ofPattern("MMM")));
            dto.setName(entry.getKey().format(DateTimeFormatter.ofPattern("MMM")));
            dto.setReachCount(entry.getValue());
            dto.setValue(entry.getValue());
            monthlyParticipationDTOs.add(dto);
        }
        return monthlyParticipationDTOs;
    }

    private Map<Platform, Integer> initPlatformIntMap() {
        Map<Platform, Integer> totals = new EnumMap<>(Platform.class);
        totals.put(Platform.LINKEDIN, 0);
        totals.put(Platform.FACEBOOK, 0);
        totals.put(Platform.INSTAGRAM, 0);
        totals.put(Platform.X, 0);
        return totals;
    }

    private LeadConversionDTO buildLeadConversionDto(Integer impressions, Integer clickCount) {
        int safeImpressions = impressions != null ? impressions : 0;
        int safeClicks = clickCount != null ? clickCount : 0;
        double conversionPercentage = safeImpressions > 0 ? (double) safeClicks * 100 / safeImpressions : 0.0;
        return new LeadConversionDTO(safeImpressions, safeClicks, conversionPercentage);
    }

    public String uploadMediaFromUrlX(PostMedia postMedia, OAuthConsumer consumer, String mediaType) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            // First, download the file from URL
            byte[] fileBytes = downloadImageFromUrl(postMedia.getFileUrl());
            String fileName = getFileNameFromUrl(postMedia.getFileUrl());
            String contentType = getContentTypeFromUrl(postMedia.getFileUrl());
            log.info("content type:{}",contentType);

            HttpPost uploadRequest = new HttpPost("https://upload.twitter.com/1.1/media/upload.json");
//            uploadRequest.setHeader("Authorization", "Bearer " + accessToken);

            // Create multipart entity with downloaded file
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addBinaryBody("media", fileBytes,
                    ContentType.create(contentType),
                    fileName);
            builder.addTextBody("media_category", getMediaCategory(contentType));

            HttpEntity multipart = builder.build();
            uploadRequest.setEntity(multipart);

            try {
                consumer.sign(uploadRequest);
            } catch (Exception e) {
                log.error("failed to sign upload request");
                throw new RuntimeException("Failed to sign OAuth request", e);
            }

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(uploadRequest)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.info("Upload response X: {}", responseBody);
                if (response.getStatusLine().getStatusCode() == 200) {
                    JsonNode jsonResponse = mapper.readTree(responseBody);
                    return jsonResponse.get("media_id_string").asText();
                } else {
                    log.error("Upload media error in X");
                    return null;
                }
            }

        } finally {
            httpClient.close();
        }
    }

    private String getFileNameFromUrl(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            String path = url.getPath();
            return path.substring(path.lastIndexOf('/') + 1);
        } catch (Exception e) {
            return "downloaded_file";
        }
    }

    private String getContentTypeFromUrl(String fileUrl) {
        String lowerUrl = fileUrl.toLowerCase();
        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) return "image/jpeg";
        if (lowerUrl.endsWith(".png")) return "image/png";
        if (lowerUrl.endsWith(".gif")) return "image/gif";
        if (lowerUrl.endsWith(".mp4")) return "video/mp4";
        if (lowerUrl.endsWith(".mov")) return "video/quicktime";
        if (lowerUrl.endsWith(".avi")) return "video/x-msvideo";
        return "application/octet-stream";
    }

    private String getMediaCategory(String contentType) {
        if (contentType.startsWith("image/")) {
            if (contentType.equals("image/gif")) {
                return "tweet_gif";
            }
            return "tweet_image";
        } else if (contentType.startsWith("video/")) {
            return "tweet_video";
        }
        return "tweet_image";
    }


    public String uploadVideoFromUrlX(PostMedia postMedia, OAuthConsumer consumer) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            // Download video from URL
            byte[] videoBytes = downloadImageFromUrl(postMedia.getFileUrl());
            String fileName = getFileNameFromUrl(postMedia.getFileUrl());

            // Step 1: INIT command - Initialize upload
            String mediaId = initializeVideoUploadX(videoBytes.length,consumer);

            // Step 2: APPEND command - Upload video in chunks
            appendVideoChunksX(mediaId, videoBytes, consumer);

            // Step 3: FINALIZE command - Finalize upload
            return finalizeVideoUploadX(mediaId, consumer);

        } finally {
            httpClient.close();
        }
    }

    private String initializeVideoUploadX(long fileSize, OAuthConsumer consumer) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            HttpPost initRequest = new HttpPost("https://upload.twitter.com/1.1/media/upload.json");

            // Create form parameters for INIT
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("command", "INIT"));
            params.add(new BasicNameValuePair("media_type", "video/mp4"));
            params.add(new BasicNameValuePair("total_bytes", String.valueOf(fileSize)));
            params.add(new BasicNameValuePair("media_category", "tweet_video"));

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
            initRequest.setEntity(entity);

            // Sign the request with OAuth 1.0a
            try {
                consumer.sign(initRequest);
            } catch (Exception e) {
                throw new RuntimeException("Failed to sign OAuth request", e);
            }

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(initRequest)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                JsonNode jsonResponse = mapper.readTree(responseBody);

                if (jsonResponse.has("media_id_string")) {
                    return jsonResponse.get("media_id_string").asText();
                } else {
                    throw new RuntimeException("INIT failed: " + responseBody);
                }
            }

        } finally {
            httpClient.close();
        }
    }

    private void appendVideoChunksX(String mediaId, byte[] videoBytes, OAuthConsumer consumer) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            // Twitter requires chunks of up to 5MB
            int chunkSize = 5 * 1024 * 1024; // 5MB
            int segmentIndex = 0;

            for (int i = 0; i < videoBytes.length; i += chunkSize) {
                int end = Math.min(videoBytes.length, i + chunkSize);
                byte[] chunk = Arrays.copyOfRange(videoBytes, i, end);

                HttpPost appendRequest = new HttpPost("https://upload.twitter.com/1.1/media/upload.json");

                // Create multipart form for APPEND
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.addTextBody("command", "APPEND");
                builder.addTextBody("media_id", mediaId);
                builder.addTextBody("segment_index", String.valueOf(segmentIndex));
                builder.addBinaryBody("media", chunk,
                        org.apache.http.entity.ContentType.APPLICATION_OCTET_STREAM,
                        "video_chunk.mp4");

                HttpEntity multipart = builder.build();
                appendRequest.setEntity(multipart);

                // Sign the request with OAuth 1.0a
                try {
                    consumer.sign(appendRequest);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to sign OAuth request", e);
                }

                // Execute append request
                try (CloseableHttpResponse response = httpClient.execute(appendRequest)) {
                    if (response.getStatusLine().getStatusCode() != 204) {
                        String responseBody = EntityUtils.toString(response.getEntity());
                        throw new RuntimeException("APPEND failed for segment " + segmentIndex + ": " + responseBody);
                    }
                }

                segmentIndex++;
            }

        } finally {
            httpClient.close();
        }
    }

    private String finalizeVideoUploadX(String mediaId, OAuthConsumer consumer) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            HttpPost finalizeRequest = new HttpPost("https://upload.twitter.com/1.1/media/upload.json");

            // Create form parameters for FINALIZE
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("command", "FINALIZE"));
            params.add(new BasicNameValuePair("media_id", mediaId));

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
            finalizeRequest.setEntity(entity);

            // Sign the request with OAuth 1.0a
            try {
                consumer.sign(finalizeRequest);
            } catch (Exception e) {
                throw new RuntimeException("Failed to sign OAuth request", e);
            }

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(finalizeRequest)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                if (response.getStatusLine().getStatusCode() == 200) {
                    JsonNode jsonResponse = mapper.readTree(responseBody);

                    // Check if video is still processing
//                    JsonNode processingInfo = jsonResponse.get("processing_info");
//                    if (processingInfo != null) {
//                        String state = processingInfo.get("state").asText();
//                        if ("pending".equals(state) || "in_progress".equals(state)) {
//                            // Wait for processing to complete
//                            return waitForVideoProcessing(mediaId, processingInfo, consumer);
//                        }
//                    }
                    try {
                        Thread.sleep(60000);
                    }catch (Exception e){
                        log.error("Thread interrupted.{}",e.getMessage());
                    }

                    return mediaId;
                } else {
                    throw new RuntimeException("FINALIZE failed: " + responseBody);
                }
            }

        } finally {
            httpClient.close();
        }
    }

    private String waitForVideoProcessing(String mediaId, JsonNode processingInfo, OAuthConsumer consumer) throws IOException {
        String state = processingInfo.get("state").asText();
        int checkAfterMs = processingInfo.has("check_after_secs") ?
                processingInfo.get("check_after_secs").asInt() * 1000 : 5000;

        while ("pending".equals(state) || "in_progress".equals(state)) {
            try {
                Thread.sleep(checkAfterMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Video processing wait interrupted", e);
            }

            // Check processing status
            String newState = checkVideoStatus(mediaId, consumer);
            if ("succeeded".equals(newState)) {
                return mediaId;
            } else if ("failed".equals(newState)) {
                throw new RuntimeException("Video processing failed");
            }

            state = newState;
        }

        return mediaId;
    }

    private String checkVideoStatus(String mediaId, OAuthConsumer consumer) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            HttpPost statusRequest = new HttpPost("https://upload.twitter.com/1.1/media/upload.json");

            // Create form parameters for STATUS
            List<BasicNameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("command", "STATUS"));
            params.add(new BasicNameValuePair("media_id", mediaId));

            log.info("Status check param: {}", params);

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
            statusRequest.setEntity(entity);

            // Sign the request with OAuth 1.0a
            try {
                consumer.sign(statusRequest);
            } catch (Exception e) {
                throw new RuntimeException("Failed to sign OAuth request", e);
            }

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(statusRequest)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                if (response.getStatusLine().getStatusCode() == 200) {
                    JsonNode jsonResponse = mapper.readTree(responseBody);
                    JsonNode processingInfo = jsonResponse.get("processing_info");
                    return processingInfo != null ? processingInfo.get("state").asText() : "succeeded";
                } else {
                    throw new RuntimeException("STATUS check failed: " + responseBody);
                }
            }

        } finally {
            httpClient.close();
        }
    }
}
