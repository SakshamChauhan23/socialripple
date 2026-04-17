package com.social.ripple.external_ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.net.URLEncoder;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetaOAuthService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String GRAPH_BASE_URL = "https://graph.facebook.com/v19.0";
    private static final String OAUTH_BASE_URL = "https://www.facebook.com/v19.0/dialog/oauth";

    @Value("${app.oauth.meta-app-id}")
    private String metaAppId;

    @Value("${app.oauth.meta-app-secret}")
    private String metaAppSecret;

    public String buildAuthorizationUrl(Platform platform, String redirectUri, String state) {
        validateMetaConfig();
        return OAUTH_BASE_URL
                + "?client_id=" + urlEncode(metaAppId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&scope=" + urlEncode(scopeFor(platform))
                + "&response_type=code"
                + "&state=" + urlEncode(state);
    }

    public MetaPersonalConnection exchangePersonalConnection(Platform platform, String code, String redirectUri) {
        String shortLivedToken = exchangeUserAccessToken(code, redirectUri);
        String userAccessToken = exchangeLongLivedToken(shortLivedToken);
        return switch (platform) {
            case FACEBOOK -> fetchFacebookPersonalConnection(userAccessToken);
            case INSTAGRAM -> fetchInstagramPersonalConnection(userAccessToken);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meta OAuth is supported only for Facebook and Instagram");
        };
    }

    public MetaBusinessConnection exchangeBusinessConnection(Platform platform, String code, String redirectUri,
                                                             String preferredExternalUserId, String preferredPageId) {
        String shortLivedToken = exchangeUserAccessToken(code, redirectUri);
        String userAccessToken = exchangeLongLivedToken(shortLivedToken);
        return switch (platform) {
            case FACEBOOK -> fetchFacebookBusinessConnection(userAccessToken, preferredExternalUserId, preferredPageId);
            case INSTAGRAM -> fetchInstagramBusinessConnection(userAccessToken, preferredExternalUserId, preferredPageId);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meta OAuth is supported only for Facebook and Instagram");
        };
    }

    private String exchangeUserAccessToken(String code, String redirectUri) {
        validateMetaConfig();
        try {
            List<NameValuePair> formParams = new ArrayList<>();
            formParams.add(new BasicNameValuePair("client_id", metaAppId));
            formParams.add(new BasicNameValuePair("client_secret", metaAppSecret));
            formParams.add(new BasicNameValuePair("redirect_uri", redirectUri));
            formParams.add(new BasicNameValuePair("code", code));

            HttpPost httpPost = new HttpPost(GRAPH_BASE_URL + "/oauth/access_token");
            httpPost.setEntity(new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8));
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setHeader("Accept", "application/json");

            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpClient.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() != 200) {
                    log.error("Meta token exchange failed | status={} | redirectUri={} | response={}",
                            response.getStatusLine().getStatusCode(), redirectUri, responseBody);
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Meta token exchange failed");
                }
                JsonNode jsonResponse = OBJECT_MAPPER.readTree(responseBody);
                JsonNode accessTokenNode = jsonResponse.get("access_token");
                if (accessTokenNode == null || !StringUtils.hasText(accessTokenNode.asText())) {
                    log.error("Meta access token missing in response | redirectUri={} | response={}", redirectUri, responseBody);
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Meta access token missing in response");
                }
                return accessTokenNode.asText();
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Meta token exchange failed", e);
        }
    }

    private String exchangeLongLivedToken(String shortLivedToken) {
        try {
            String url = GRAPH_BASE_URL + "/oauth/access_token"
                    + "?grant_type=fb_exchange_token"
                    + "&client_id=" + urlEncode(metaAppId)
                    + "&client_secret=" + urlEncode(metaAppSecret)
                    + "&fb_exchange_token=" + urlEncode(shortLivedToken);

            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Accept", "application/json");

            try (CloseableHttpClient httpClient = HttpClients.createDefault();
                 CloseableHttpResponse response = httpClient.execute(httpGet)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() != 200) {
                    log.warn("Meta long-lived token exchange failed, using short-lived | status={} | response={}",
                            response.getStatusLine().getStatusCode(), responseBody);
                    return shortLivedToken;
                }
                JsonNode jsonResponse = OBJECT_MAPPER.readTree(responseBody);
                String longLivedToken = jsonResponse.path("access_token").asText(null);
                if (!StringUtils.hasText(longLivedToken)) {
                    log.warn("Meta long-lived token missing in response, using short-lived");
                    return shortLivedToken;
                }
                long expiresIn = jsonResponse.path("expires_in").asLong(0);
                log.info("Meta long-lived token obtained | expiresIn={}s (~{}d)", expiresIn, expiresIn / 86400);
                return longLivedToken;
            }
        } catch (Exception e) {
            log.warn("Meta long-lived token exchange error, using short-lived: {}", e.getMessage());
            return shortLivedToken;
        }
    }

    private MetaPersonalConnection fetchFacebookPersonalConnection(String userAccessToken) {
        // Use /me/accounts to get the PAGE ID the user manages — the token has page-level
        // permissions (pages_read_engagement etc.), NOT personal profile permissions (user_posts).
        // Leader fetch calls /{page-id}/posts which requires page permissions.
        JsonNode accounts = getJson(buildGraphUrl("/me/accounts", "id,name,access_token", userAccessToken)).path("data");
        if (accounts.isArray() && !accounts.isEmpty()) {
            JsonNode firstPage = accounts.get(0);
            String pageId = firstPage.path("id").asText(null);
            String pageToken = firstPage.path("access_token").asText(userAccessToken);
            if (pageId != null && !pageId.isBlank()) {
                log.info("Facebook personal connection resolved via page | pageId={} | name={}", pageId, firstPage.path("name").asText(""));
                return new MetaPersonalConnection(pageId, pageToken);
            }
        }
        // Fallback to personal profile if no pages found
        JsonNode me = getJson(buildGraphUrl("/me", "id,name", userAccessToken));
        String userId = me.path("id").asText(null);
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Facebook profile ID not found");
        }
        log.info("Facebook personal connection resolved via profile (no pages) | userId={}", userId);
        return new MetaPersonalConnection(userId, userAccessToken);
    }

    private MetaPersonalConnection fetchInstagramPersonalConnection(String userAccessToken) {
        try {
            MetaBusinessConnection businessConnection = fetchInstagramBusinessConnection(userAccessToken, null, null);
            return new MetaPersonalConnection(businessConnection.getExternalUserId(), businessConnection.getAccessToken());
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST
                    && e.getReason() != null && e.getReason().contains("No Instagram business accounts")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Instagram personal accounts are not supported. Please convert to a Business or Creator account in Instagram settings, then reconnect.");
            }
            throw e;
        }
    }

    private MetaBusinessConnection fetchFacebookBusinessConnection(String userAccessToken, String preferredExternalUserId, String preferredPageId) {
        JsonNode accounts = getJson(buildGraphUrl("/me/accounts",
                        "id,name,username,link,access_token",
                        userAccessToken))
                .path("data");
        if (!(accounts instanceof ArrayNode arrayNode) || arrayNode.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No Facebook pages found for this Meta account");
        }
        JsonNode selectedPage = selectValidatedFacebookPage(arrayNode, preferredExternalUserId, preferredPageId);
        String pageId = selectedPage.path("id").asText(null);
        String accessToken = selectedPage.path("access_token").asText(null);
        if (!StringUtils.hasText(pageId) || !StringUtils.hasText(accessToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Facebook page token or page id missing");
        }
        return MetaBusinessConnection.builder()
                .externalUserId(pageId)
                .pageId(pageId)
                .accessToken(accessToken)
                .username(selectedPage.path("username").asText(null))
                .pageUrl(selectedPage.path("link").asText(null))
                .displayName(firstNonBlank(selectedPage.path("name").asText(null), selectedPage.path("username").asText(null)))
                .build();
    }

    private MetaBusinessConnection fetchInstagramBusinessConnection(String userAccessToken, String preferredExternalUserId, String preferredPageId) {
        JsonNode accounts = getJson(buildGraphUrl("/me/accounts",
                        "name,id,link,access_token,instagram_business_account{id,username,name,profile_picture_url}",
                        userAccessToken))
                .path("data");
        if (!(accounts instanceof ArrayNode arrayNode) || arrayNode.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No Instagram business accounts found for this Meta account");
        }
        JsonNode selectedPage = selectValidatedInstagramBusinessPage(arrayNode, preferredExternalUserId, preferredPageId);
        JsonNode instagramNode = selectedPage.path("instagram_business_account");
        String instagramId = instagramNode.path("id").asText(null);
        String pageId = selectedPage.path("id").asText(null);
        String accessToken = selectedPage.path("access_token").asText(null);
        if (!StringUtils.hasText(instagramId) || !StringUtils.hasText(accessToken)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Instagram business account or token missing");
        }
        String username = instagramNode.path("username").asText(null);
        return MetaBusinessConnection.builder()
                .externalUserId(instagramId)
                .pageId(pageId)
                .accessToken(accessToken)
                .username(username)
                .pageUrl(StringUtils.hasText(username) ? "https://www.instagram.com/" + username + "/" : selectedPage.path("link").asText(null))
                .displayName(firstNonBlank(instagramNode.path("name").asText(null), selectedPage.path("name").asText(null), username))
                .build();
    }

    private JsonNode selectMatchingPage(ArrayNode accounts, String preferredExternalUserId, String preferredPageId, String nestedNodeName) {
        for (JsonNode page : accounts) {
            JsonNode nested = StringUtils.hasText(nestedNodeName) ? page.path(nestedNodeName) : page;
            String currentExternalId = nested.path("id").asText(null);
            String currentPageId = page.path("id").asText(null);
            if (StringUtils.hasText(preferredExternalUserId) && preferredExternalUserId.equals(currentExternalId)) {
                return page;
            }
            if (StringUtils.hasText(preferredPageId) && preferredPageId.equals(currentPageId)) {
                return page;
            }
        }
        for (JsonNode page : accounts) {
            if (!StringUtils.hasText(nestedNodeName) || !page.path(nestedNodeName).isMissingNode()) {
                return page;
            }
        }
        return accounts.get(0);
    }

    private JsonNode selectInstagramBusinessPage(ArrayNode accounts, String preferredExternalUserId, String preferredPageId) {
        for (JsonNode page : accounts) {
            JsonNode instagramNode = page.path("instagram_business_account");
            String instagramId = instagramNode.path("id").asText(null);
            String currentPageId = page.path("id").asText(null);
            String accessToken = page.path("access_token").asText(null);
            if (StringUtils.hasText(preferredExternalUserId)
                    && preferredExternalUserId.equals(instagramId)
                    && StringUtils.hasText(accessToken)) {
                return page;
            }
            if (StringUtils.hasText(preferredPageId)
                    && preferredPageId.equals(currentPageId)
                    && StringUtils.hasText(instagramId)
                    && StringUtils.hasText(accessToken)) {
                return page;
            }
        }
        for (JsonNode page : accounts) {
            JsonNode instagramNode = page.path("instagram_business_account");
            String instagramId = instagramNode.path("id").asText(null);
            String accessToken = page.path("access_token").asText(null);
            if (StringUtils.hasText(instagramId) && StringUtils.hasText(accessToken)) {
                return page;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No Instagram business accounts found for this Meta account");
    }

    private JsonNode selectValidatedFacebookPage(ArrayNode accounts, String preferredExternalUserId, String preferredPageId) {
        List<JsonNode> candidates = orderedCandidates(accounts, preferredExternalUserId, preferredPageId, null);
        for (JsonNode page : candidates) {
            String pageId = page.path("id").asText(null);
            String pageAccessToken = page.path("access_token").asText(null);
            if (!StringUtils.hasText(pageId) || !StringUtils.hasText(pageAccessToken)) {
                continue;
            }
            if (canReadFacebookPosts(pageId, pageAccessToken)) {
                return page;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fetchable Facebook pages found for this Meta account");
    }

    private JsonNode selectValidatedInstagramBusinessPage(ArrayNode accounts, String preferredExternalUserId, String preferredPageId) {
        List<JsonNode> candidates = orderedCandidates(accounts, preferredExternalUserId, preferredPageId, "instagram_business_account");
        for (JsonNode page : candidates) {
            JsonNode instagramNode = page.path("instagram_business_account");
            String instagramId = instagramNode.path("id").asText(null);
            String pageAccessToken = page.path("access_token").asText(null);
            if (!StringUtils.hasText(instagramId) || !StringUtils.hasText(pageAccessToken)) {
                continue;
            }
            if (canReadInstagramMedia(instagramId, pageAccessToken)) {
                return page;
            }
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fetchable Instagram business accounts found for this Meta account");
    }

    private List<JsonNode> orderedCandidates(ArrayNode accounts, String preferredExternalUserId, String preferredPageId, String nestedNodeName) {
        List<JsonNode> ordered = new ArrayList<>();
        JsonNode preferred = null;
        for (JsonNode page : accounts) {
            JsonNode nested = StringUtils.hasText(nestedNodeName) ? page.path(nestedNodeName) : page;
            String currentExternalId = nested.path("id").asText(null);
            String currentPageId = page.path("id").asText(null);
            if ((StringUtils.hasText(preferredExternalUserId) && preferredExternalUserId.equals(currentExternalId))
                    || (StringUtils.hasText(preferredPageId) && preferredPageId.equals(currentPageId))) {
                preferred = page;
                break;
            }
        }
        if (preferred != null) {
            ordered.add(preferred);
        }
        for (JsonNode page : accounts) {
            if (preferred != null && Objects.equals(page.path("id").asText(null), preferred.path("id").asText(null))) {
                continue;
            }
            if (!StringUtils.hasText(nestedNodeName) || !page.path(nestedNodeName).isMissingNode()) {
                ordered.add(page);
            }
        }
        return ordered;
    }

    private boolean canReadFacebookPosts(String pageId, String accessToken) {
        try {
            getJson(buildGraphReadUrl("/" + pageId + "/posts", accessToken));
            return true;
        } catch (ResponseStatusException ex) {
            log.warn("Facebook page validation failed | pageId={} | reason={}", pageId, ex.getReason());
            return false;
        }
    }

    private boolean canReadInstagramMedia(String instagramId, String accessToken) {
        try {
            getJson(buildGraphReadUrl("/" + instagramId + "/media", accessToken));
            return true;
        } catch (ResponseStatusException ex) {
            log.warn("Instagram account validation failed | instagramId={} | reason={}", instagramId, ex.getReason());
            return false;
        }
    }

    private String buildGraphReadUrl(String path, String accessToken) {
        try {
            return new URIBuilder(GRAPH_BASE_URL + path)
                    .addParameter("fields", "id")
                    .addParameter("limit", "1")
                    .addParameter("access_token", accessToken)
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Meta Graph request failed", e);
        }
    }

    private String buildGraphUrl(String path, String fields, String accessToken) {
        try {
            return new URIBuilder(GRAPH_BASE_URL + path)
                    .addParameter("fields", fields)
                    .addParameter("access_token", accessToken)
                    .build()
                    .toString();
        } catch (URISyntaxException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Meta Graph request failed", e);
        }
    }

    private JsonNode getJson(String url) {
        try {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("Accept", "application/json");
            try (CloseableHttpClient client = HttpClients.createDefault();
                 CloseableHttpResponse response = client.execute(httpGet)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                if (response.getStatusLine().getStatusCode() != 200) {
                    log.error("Meta Graph request failed | status={} | url={} | response={}",
                            response.getStatusLine().getStatusCode(), url, responseBody);
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Meta Graph request failed");
                }
                return OBJECT_MAPPER.readTree(responseBody);
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Meta Graph request failed", e);
        }
    }

    private void validateMetaConfig() {
        if (!StringUtils.hasText(metaAppId) || !StringUtils.hasText(metaAppSecret)) {
            log.error("Meta app credentials are not configured | appIdPresent={} | appSecretPresent={}",
                    StringUtils.hasText(metaAppId), StringUtils.hasText(metaAppSecret));
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Meta app credentials are not configured");
        }
    }

    private String scopeFor(Platform platform) {
        return switch (platform) {
            case FACEBOOK -> "pages_show_list,pages_read_engagement,pages_manage_posts,pages_manage_metadata,pages_read_user_content,read_insights";
            case INSTAGRAM -> "instagram_basic,instagram_manage_insights,instagram_content_publish,pages_show_list,pages_manage_posts,pages_manage_metadata,pages_read_engagement,read_insights";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Meta OAuth is supported only for Facebook and Instagram");
        };
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    @Getter
    @Builder
    public static class MetaBusinessConnection {
        private final String externalUserId;
        private final String pageId;
        private final String accessToken;
        private final String username;
        private final String pageUrl;
        private final String displayName;
    }

    @Getter
    @RequiredArgsConstructor
    public static class MetaPersonalConnection {
        private final String externalUserId;
        private final String accessToken;
    }
}
