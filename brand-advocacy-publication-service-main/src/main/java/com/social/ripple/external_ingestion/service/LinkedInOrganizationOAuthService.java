package com.social.ripple.external_ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.ripple.external_ingestion.dto.response.BusinessPageOptionDto;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class LinkedInOrganizationOAuthService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String AUTH_URL = "https://www.linkedin.com/oauth/v2/authorization";
    private static final String TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final String USERINFO_URL = "https://api.linkedin.com/v2/userinfo";
    private static final String ORGANIZATION_AUTHORIZATION_URL = "https://api.linkedin.com/rest/organizationAuthorizations";
    private static final String ORGANIZATION_URL = "https://api.linkedin.com/v2/organizations/%s?projection=(id,localizedName,vanityName)";
    private static final String LINKEDIN_VERSION_DEFAULT = "202603";

    @Value("${app.oauth.linkedin-org-client-id:}")
    private String clientId;

    @Value("${app.oauth.linkedin-org-client-secret:}")
    private String clientSecret;

    @Value("${app.oauth.linkedin-scope:openid profile w_member_social rw_organization_admin r_organization_social}")
    private String scope;

    @Value("${app.oauth.linkedin-org-scope:r_organization_social rw_organization_admin w_organization_social}")
    private String orgScope;

    @Value("${app.oauth.linkedin-version:" + LINKEDIN_VERSION_DEFAULT + "}")
    private String linkedInVersion;

    public String buildAuthorizationUrl(String redirectUri, String state) {
        validateConfig();
        return AUTH_URL
                + "?response_type=code"
                + "&client_id=" + urlEncode(clientId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&scope=" + urlEncode(scope)
                + "&state=" + urlEncode(state);
    }

    public String buildOrgAuthorizationUrl(String redirectUri, String state) {
        validateConfig();
        return AUTH_URL
                + "?response_type=code"
                + "&client_id=" + urlEncode(clientId)
                + "&redirect_uri=" + urlEncode(redirectUri)
                + "&scope=" + urlEncode(orgScope)
                + "&state=" + urlEncode(state);
    }

    public LinkedInDiscoveryResult exchangeAndDiscover(String code, String redirectUri) {
        validateConfig();
        try {
            LinkedInTokenResponse tokenResponse = exchangeToken(code, redirectUri);
            String personId = fetchPersonId(tokenResponse.accessToken());
            List<String> organizationIds = fetchOrganizationIds(tokenResponse.accessToken(), personId);
            if (organizationIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No LinkedIn organization pages found for this account");
            }

            List<BusinessPageOptionDto> pages = new ArrayList<>();
            for (String organizationId : organizationIds) {
                BusinessPageOptionDto page = fetchOrganization(tokenResponse.accessToken(), organizationId);
                if (page != null) {
                    pages.add(page);
                }
            }

            if (pages.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No LinkedIn organization pages found for this account");
            }
            return new LinkedInDiscoveryResult(tokenResponse.accessToken(), tokenResponse.refreshToken(), personId, pages);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LinkedIn OAuth flow failed", ex);
        }
    }

    public LinkedInDiscoveryResult exchangeAndDiscoverWithoutSignIn(String code, String redirectUri) {
        validateConfig();
        try {
            LinkedInTokenResponse tokenResponse = exchangeToken(code, redirectUri);
            // Skip fetchPersonId — requires openid scope which App 2 doesn't have
            // Pass null personId — org discovery will skip person filter
            List<String> organizationIds = fetchOrganizationIds(tokenResponse.accessToken(), null);
            if (organizationIds.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No LinkedIn organization pages found for this account. Ensure you are an admin of a LinkedIn company page.");
            }

            List<BusinessPageOptionDto> pages = new ArrayList<>();
            for (String organizationId : organizationIds) {
                BusinessPageOptionDto page = fetchOrganization(tokenResponse.accessToken(), organizationId);
                if (page != null) {
                    pages.add(page);
                }
            }

            if (pages.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No LinkedIn organization pages found for this account");
            }
            return new LinkedInDiscoveryResult(tokenResponse.accessToken(), tokenResponse.refreshToken(), null, pages);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LinkedIn organization OAuth flow failed: " + ex.getMessage(), ex);
        }
    }

    private LinkedInTokenResponse exchangeToken(String code, String redirectUri) throws Exception {
        List<NameValuePair> formParams = new ArrayList<>();
        formParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
        formParams.add(new BasicNameValuePair("code", code));
        formParams.add(new BasicNameValuePair("redirect_uri", redirectUri));
        formParams.add(new BasicNameValuePair("client_id", clientId));
        formParams.add(new BasicNameValuePair("client_secret", clientSecret));

        HttpPost request = new HttpPost(TOKEN_URL);
        request.setEntity(new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8));
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        request.setHeader("Accept", "application/json");

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            String body = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LinkedIn token exchange failed");
            }
            JsonNode json = OBJECT_MAPPER.readTree(body);
            String accessToken = text(json, "access_token");
            String refreshToken = text(json, "refresh_token");
            if (!StringUtils.hasText(accessToken)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LinkedIn access token missing in response");
            }
            return new LinkedInTokenResponse(accessToken, refreshToken);
        }
    }

    private String fetchPersonId(String accessToken) throws Exception {
        HttpGet request = new HttpGet(USERINFO_URL);
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Accept", "application/json");

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            String body = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LinkedIn user profile request failed");
            }
            JsonNode json = OBJECT_MAPPER.readTree(body);
            String subject = text(json, "sub");
            if (!StringUtils.hasText(subject)) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LinkedIn user profile id missing");
            }
            return subject;
        }
    }

    private List<String> fetchOrganizationIds(String accessToken, String personId) throws Exception {
        // Try the REST API first (organizationAuthorizations)
        try {
            List<String> ids = fetchOrganizationIdsViaRestApi(accessToken, personId);
            if (!ids.isEmpty()) {
                return ids;
            }
        } catch (Exception ex) {
            // Fall through to legacy API
        }

        // Fallback: use organizationalEntityAcls (v2 API, simpler)
        return fetchOrganizationIdsViaAcls(accessToken);
    }

    private List<String> fetchOrganizationIdsViaRestApi(String accessToken, String personId) throws Exception {
        String url = ORGANIZATION_AUTHORIZATION_URL
                + "?bq=authorizationActionsAndImpersonator"
                + "&authorizationActions=List("
                + "(authorizationAction:(organizationProfileAuthorizationAction:(actionType:ADMINISTRATION_PAGE_VIEW))),"
                + "(authorizationAction:(organizationContentAuthorizationAction:(actionType:ORGANIC_SHARE_CREATE)))"
                + ")";
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Accept", "application/json");
        request.setHeader("X-Restli-Protocol-Version", "2.0.0");
        request.setHeader("Linkedin-Version", linkedInVersionHeader());

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            String body = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new RuntimeException("LinkedIn organizationAuthorizations returned HTTP " + statusCode + ": " + body);
            }
            JsonNode json = OBJECT_MAPPER.readTree(body);
            JsonNode outerElements = json.path("elements");
            Set<String> organizationIds = new LinkedHashSet<>();
            if (outerElements.isArray()) {
                for (JsonNode actionBucket : outerElements) {
                    JsonNode elements = actionBucket.path("elements");
                    if (!elements.isArray()) {
                        continue;
                    }
                    for (JsonNode element : elements) {
                        if (!isApprovedAuthorization(element)) {
                            continue;
                        }
                        String impersonator = text(element, "impersonator");
                        if (personId != null && StringUtils.hasText(impersonator) && !personMatches(personId, impersonator)) {
                            continue;
                        }
                        String organizationUrn = text(element, "organization");
                        String organizationId = toOrganizationId(organizationUrn);
                        if (StringUtils.hasText(organizationId)) {
                            organizationIds.add(organizationId);
                        }
                    }
                }
            }
            return new ArrayList<>(organizationIds);
        }
    }

    private List<String> fetchOrganizationIdsViaAcls(String accessToken) throws Exception {
        String url = "https://api.linkedin.com/v2/organizationalEntityAcls?q=roleAssignee&role=ADMINISTRATOR&projection=(elements*(organizationalTarget))";
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Accept", "application/json");
        request.setHeader("X-Restli-Protocol-Version", "2.0.0");

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            String body = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "LinkedIn organization discovery failed (ACLs API HTTP " + statusCode + "): " + body);
            }
            JsonNode json = OBJECT_MAPPER.readTree(body);
            JsonNode elements = json.path("elements");
            Set<String> organizationIds = new LinkedHashSet<>();
            if (elements.isArray()) {
                for (JsonNode element : elements) {
                    String orgTarget = text(element, "organizationalTarget");
                    if (StringUtils.hasText(orgTarget)) {
                        String orgId = orgTarget.replace("urn:li:organization:", "").trim();
                        if (StringUtils.hasText(orgId)) {
                            organizationIds.add(orgId);
                        }
                    }
                }
            }
            return new ArrayList<>(organizationIds);
        }
    }

    private BusinessPageOptionDto fetchOrganization(String accessToken, String organizationId) throws Exception {
        HttpGet request = new HttpGet(String.format(ORGANIZATION_URL, urlEncode(organizationId)));
        request.setHeader("Authorization", "Bearer " + accessToken);
        request.setHeader("Accept", "application/json");
        request.setHeader("X-Restli-Protocol-Version", "2.0.0");
        request.setHeader("Linkedin-Version", linkedInVersionHeader());

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            String body = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200) {
                return BusinessPageOptionDto.builder()
                        .selectionId(organizationId)
                        .platform("LINKEDIN")
                        .externalUserId(organizationId)
                        .displayName("LinkedIn Organization " + organizationId)
                        .pageUrl("https://www.linkedin.com/company/" + organizationId)
                        .businessPageLink("https://www.linkedin.com/company/" + organizationId)
                        .build();
            }
            JsonNode json = OBJECT_MAPPER.readTree(body);
            String vanityName = text(json, "vanityName");
            String localizedName = text(json, "localizedName");
            String pageUrl = StringUtils.hasText(vanityName)
                    ? "https://www.linkedin.com/company/" + vanityName
                    : "https://www.linkedin.com/company/" + organizationId;
            return BusinessPageOptionDto.builder()
                    .selectionId(organizationId)
                    .platform("LINKEDIN")
                    .externalUserId(organizationId)
                    .username(vanityName)
                    .displayName(StringUtils.hasText(localizedName) ? localizedName : "LinkedIn Organization " + organizationId)
                    .pageUrl(pageUrl)
                    .businessPageLink(pageUrl)
                    .build();
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "LinkedIn business OAuth credentials are not configured");
        }
    }

    private boolean isApprovedAuthorization(JsonNode element) {
        JsonNode statusNode = element.path("status");
        return statusNode.has("com.linkedin.organization.Approved");
    }

    private boolean personMatches(String personId, String impersonatorUrn) {
        return ("urn:li:person:" + personId).equalsIgnoreCase(impersonatorUrn);
    }

    private String toOrganizationId(String organizationUrn) {
        if (!StringUtils.hasText(organizationUrn)) {
            return null;
        }
        return organizationUrn.replace("urn:li:organization:", "").trim();
    }

    private String linkedInVersionHeader() {
        return StringUtils.hasText(linkedInVersion)
                ? linkedInVersion.trim()
                : LINKEDIN_VERSION_DEFAULT;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    public record LinkedInDiscoveryResult(
            String accessToken,
            String refreshToken,
            String personId,
            List<BusinessPageOptionDto> pages
    ) {
    }

    private record LinkedInTokenResponse(String accessToken, String refreshToken) {
    }
}
