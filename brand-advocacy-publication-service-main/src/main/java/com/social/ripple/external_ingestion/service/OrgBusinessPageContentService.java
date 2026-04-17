package com.social.ripple.external_ingestion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.ripple.external_ingestion.dao.model.ExternalPlatform;
import com.social.ripple.external_ingestion.dao.repository.ExternalPlatformRepository;
import com.social.ripple.external_ingestion.util.enumeration.Platform;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OrgBusinessPageContentService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ExternalPlatformRepository externalPlatformRepository;

    public String composeBusinessPageContent(Long organizationId, Platform platform, String baseContent) {
        String normalizedBaseContent = baseContent == null ? "" : baseContent.trim();
        if (organizationId == null || platform == null) {
            return normalizedBaseContent;
        }

        String suffix = externalPlatformRepository
                .findByOrganizationIdAndPlatformNameIgnoreCase(organizationId, platform.getDisplayName())
                .map(ExternalPlatform::getCredentials)
                .map(credentials -> resolveSuffix(credentials, platform))
                .orElse("");

        if (!StringUtils.hasText(suffix)) {
            return normalizedBaseContent;
        }
        if (!StringUtils.hasText(normalizedBaseContent)) {
            return suffix;
        }
        if (normalizedBaseContent.contains(suffix)) {
            return normalizedBaseContent;
        }
        return normalizedBaseContent + "\n\n" + suffix;
    }

    private String resolveSuffix(String credentialsJson, Platform platform) {
        if (!StringUtils.hasText(credentialsJson)) {
            return "";
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(credentialsJson);
            String username = firstNonBlank(text(node, "username"), text(node, "screenName"), text(node, "handle"));
            String pageUrl = firstNonBlank(
                    text(node, "businessPageLink"),
                    text(node, "business_page_link"),
                    text(node, "pageUrl"),
                    text(node, "page_url"),
                    text(node, "url"),
                    text(node, "link")
            );
            // NOTE: keep in sync with OrganizationPlatformSettingsService.contentSuffix
            // and PlatformBusinessPageComposer.resolveSuffix. Tracked tech debt: consolidate.
            return switch (platform) {
                case X, INSTAGRAM -> StringUtils.hasText(username) ? "@" + username : pageUrl;
                case FACEBOOK -> StringUtils.hasText(username)
                        ? "@" + username
                        : (StringUtils.hasText(pageUrl) ? "Follow Us: " + pageUrl : "");
                case LINKEDIN -> StringUtils.hasText(pageUrl) ? "Follow Us: " + pageUrl : "";
            };
        } catch (Exception ignored) {
            return "";
        }
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || fieldName == null) {
            return "";
        }
        JsonNode fieldNode = node.get(fieldName);
        if (fieldNode == null || fieldNode.isNull()) {
            return "";
        }
        String value = fieldNode.asText("");
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }
}
