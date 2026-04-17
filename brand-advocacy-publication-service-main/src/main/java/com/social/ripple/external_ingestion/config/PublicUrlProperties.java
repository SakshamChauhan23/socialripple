package com.social.ripple.external_ingestion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublicUrlProperties {
    private static final String ADMIN_SETTINGS = "ADMIN_SETTINGS";

    @Value("${app.public.frontend-base-url:https://dashboard.socialripple.ai}")
    private String frontendBaseUrl;

    @Value("${app.public.api-base-url:https://api.socialripple.ai}")
    private String apiBaseUrl;

    @Value("${app.public.notification-callback-url:${app.public.api-base-url}/v1/websocket/message}")
    private String notificationCallbackUrl;

    @Value("${app.oauth.x-callback-url:${app.public.api-base-url}/external-ingestion/v1/api/x/callback}")
    private String xCallbackUrl;

    @Value("${app.oauth.linkedin-callback-url:${app.public.api-base-url}/external-ingestion/v1/api/linkedin/callback}")
    private String linkedInCallbackUrl;

    @Value("${app.oauth.facebook-callback-url:${app.public.api-base-url}/external-ingestion/v1/api/fb/callback}")
    private String facebookCallbackUrl;

    @Value("${app.oauth.instagram-callback-url:${app.public.api-base-url}/external-ingestion/v1/api/instagram/callback}")
    private String instagramCallbackUrl;

    @Value("${app.oauth.org-facebook-callback-url:${app.public.api-base-url}/external-ingestion/v1/api/org/business-pages/facebook/callback}")
    private String organizationFacebookCallbackUrl;

    @Value("${app.oauth.org-instagram-callback-url:${app.public.api-base-url}/external-ingestion/v1/api/org/business-pages/instagram/callback}")
    private String organizationInstagramCallbackUrl;

    @Value("${app.oauth.org-linkedin-callback-url:${app.public.api-base-url}/external-ingestion/v1/api/org/business-pages/linkedin/callback}")
    private String organizationLinkedInCallbackUrl;

    @Value("${app.oauth.org-x-callback-url:${app.public.api-base-url}/external-ingestion/v1/api/org/business-pages/x/callback}")
    private String organizationXCallbackUrl;

    public String getFrontendSettingsUrl() {
        return trimTrailingSlash(frontendBaseUrl) + "/employees/settings";
    }

    public String getFrontendAdminSettingsUrl() {
        return trimTrailingSlash(frontendBaseUrl) + "/admin/settings";
    }

    public String resolveFrontendSettingsUrl(String oauthSourcePage) {
        if (ADMIN_SETTINGS.equalsIgnoreCase(oauthSourcePage)) {
            return getFrontendAdminSettingsUrl();
        }
        return getFrontendSettingsUrl();
    }

    public String getNotificationCallbackUrl() {
        return notificationCallbackUrl;
    }

    public String getXCallbackUrl() {
        return xCallbackUrl;
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

    public String getOrganizationFacebookCallbackUrl() {
        return organizationFacebookCallbackUrl;
    }

    public String getOrganizationInstagramCallbackUrl() {
        return organizationInstagramCallbackUrl;
    }

    public String getOrganizationLinkedInCallbackUrl() {
        return organizationLinkedInCallbackUrl;
    }

    public String getOrganizationXCallbackUrl() {
        return organizationXCallbackUrl;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
