package com.social.ripple.usermanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublicUrlProperties {

    @Value("${app.public.frontend-base-url:https://dashboard.socialripple.ai}")
    private String frontendBaseUrl;

    @Value("${app.public.api-base-url:https://api.socialripple.ai}")
    private String apiBaseUrl;

    @Value("${app.oauth.linkedin-callback-url:${app.public.api-base-url}/external-ingestion/v1/api/linkedin/callback}")
    private String linkedInCallbackUrl;

    @Value("${app.oauth.facebook-callback-url:${app.public.api-base-url}/external-ingestion/v1/api/fb/callback}")
    private String facebookCallbackUrl;

    @Value("${app.oauth.instagram-callback-url:${app.public.api-base-url}/external-ingestion/v1/api/instagram/callback}")
    private String instagramCallbackUrl;

    public String getFrontendBaseUrl() {
        return trimTrailingSlash(frontendBaseUrl);
    }

    public String getApiBaseUrl() {
        return trimTrailingSlash(apiBaseUrl);
    }

    public String getLinkedInCallbackUrl() {
        return linkedInCallbackUrl;
    }

    public String getFacebookCallbackUrl() {
        return facebookCallbackUrl;
    }

    public String getInstagramCallbackUrl() {
        return instagramCallbackUrl;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
