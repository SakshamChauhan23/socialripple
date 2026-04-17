package com.social.ripple.usermanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.social.ripple.usermanagement.dao.model.ExternalPlatform;
import com.social.ripple.usermanagement.dao.repository.ExternalPlatformRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Computes per-platform character budgets for AI content generation.
 * <p>
 * The budget is the platform's absolute character limit minus the space reserved
 * for the business-page suffix ("Follow Us: &lt;url&gt;" or "@username") that is
 * appended automatically at publish time by the publication-service.
 * <p>
 * Platform limits (canonical source: PlatformBusinessPageComposer.validateLength
 * in brand-advocacy-publication-service):
 * <ul>
 *     <li>X: 280</li>
 *     <li>LinkedIn: 3000</li>
 *     <li>Instagram: 2200</li>
 *     <li>Facebook: 63204</li>
 * </ul>
 * Since "common" content is sent to LinkedIn AND Instagram AND Facebook, the
 * effective common limit is the min across those three (Instagram's 2200).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentLimitCalculator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Platform absolute character limits (keep in sync with PlatformBusinessPageComposer.validateLength)
    private static final int X_LIMIT = 280;
    private static final int LINKEDIN_LIMIT = 3000;
    private static final int INSTAGRAM_LIMIT = 2200;
    private static final int FACEBOOK_LIMIT = 63204;

    private static final String FOLLOW_US_PREFIX = "Follow Us: ";
    private static final int SAFETY_MARGIN = 5;

    // Fallback budgets when no org data is available
    private static final int DEFAULT_COMMON_MAX = 2050;
    private static final int DEFAULT_X_MAX = 200;

    private final ExternalPlatformRepository externalPlatformRepository;

    public PlatformBudgets calculateFor(Long organizationId) {
        if (organizationId == null) {
            return new PlatformBudgets(DEFAULT_COMMON_MAX, DEFAULT_X_MAX);
        }

        List<ExternalPlatform> platforms = externalPlatformRepository.findByOrganizationId(organizationId);
        if (platforms == null || platforms.isEmpty()) {
            return new PlatformBudgets(DEFAULT_COMMON_MAX, DEFAULT_X_MAX);
        }

        int worstCaseCommonSuffix = 0;
        int xSuffixLen = 0;

        for (ExternalPlatform ep : platforms) {
            String name = ep.getPlatformName();
            if (name == null) continue;

            String credentials = ep.getCredentials();
            String username = extractField(credentials, "username", "screenName", "handle", "userName");
            String pageUrl = extractField(credentials, "businessPageLink", "business_page_link",
                    "pageUrl", "page_url", "url", "link");

            String normalizedName = name.trim().toUpperCase();

            switch (normalizedName) {
                case "X", "TWITTER" -> {
                    // X uses @username (no "Follow Us:")
                    if (StringUtils.hasText(username)) {
                        xSuffixLen = Math.max(xSuffixLen, ("@" + username).length() + 2); // +2 for blank line separator
                    }
                }
                case "LINKEDIN" -> {
                    // LinkedIn always uses "Follow Us: <url>"
                    if (StringUtils.hasText(pageUrl)) {
                        int suffixLen = (FOLLOW_US_PREFIX + pageUrl).length() + 2;
                        worstCaseCommonSuffix = Math.max(worstCaseCommonSuffix, suffixLen);
                    }
                }
                case "FACEBOOK" -> {
                    // Facebook: @username first (short), URL fallback (longer with "Follow Us:")
                    if (StringUtils.hasText(username)) {
                        int suffixLen = ("@" + username).length() + 2;
                        worstCaseCommonSuffix = Math.max(worstCaseCommonSuffix, suffixLen);
                    } else if (StringUtils.hasText(pageUrl)) {
                        int suffixLen = (FOLLOW_US_PREFIX + pageUrl).length() + 2;
                        worstCaseCommonSuffix = Math.max(worstCaseCommonSuffix, suffixLen);
                    }
                }
                case "INSTAGRAM" -> {
                    // Instagram uses @username (no "Follow Us:")
                    if (StringUtils.hasText(username)) {
                        int suffixLen = ("@" + username).length() + 2;
                        worstCaseCommonSuffix = Math.max(worstCaseCommonSuffix, suffixLen);
                    }
                }
                default -> { /* ignore unknown platforms */ }
            }
        }

        // common = strictest platform (IG 2200) minus worst-case suffix across all common platforms
        int commonMax = INSTAGRAM_LIMIT - worstCaseCommonSuffix - SAFETY_MARGIN;
        if (commonMax < 100) {
            commonMax = DEFAULT_COMMON_MAX; // something went wrong; use safe fallback
        }

        int xMax = X_LIMIT - xSuffixLen - SAFETY_MARGIN;
        if (xMax < 50) {
            xMax = DEFAULT_X_MAX;
        }

        log.debug("ContentLimitCalculator|orgId={}|commonMax={}|xMax={}|worstCaseCommonSuffix={}|xSuffixLen={}",
                organizationId, commonMax, xMax, worstCaseCommonSuffix, xSuffixLen);

        return new PlatformBudgets(commonMax, xMax);
    }

    private String extractField(String credentialsJson, String... fieldNames) {
        if (!StringUtils.hasText(credentialsJson) || fieldNames == null) {
            return null;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(credentialsJson);
            if (node == null || node.isNull()) return null;
            for (String field : fieldNames) {
                JsonNode fieldNode = node.get(field);
                if (fieldNode != null && !fieldNode.isNull()) {
                    String value = fieldNode.asText("");
                    if (StringUtils.hasText(value)) return value.trim();
                }
            }
        } catch (Exception ignored) {
            // malformed JSON in credentials — fall through
        }
        return null;
    }

    public record PlatformBudgets(int commonMax, int xMax) {}
}
